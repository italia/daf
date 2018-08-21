package it.gov.daf.securitymanager.service

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import it.gov.daf.securitymanager.service.utilities.ConfigReader
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import security_manager.yaml.{Error, IpaUser, Success}
import cats.implicits._
import org.apache.commons.lang3.StringEscapeUtils
import play.api.Logger
import play.api.libs.functional.syntax._

import scala.concurrent.Future

@Singleton
class SupersetApiClient @Inject()(secInvokeManager: SecuredInvocationManager){

  import play.api.libs.concurrent.Execution.Implicits._

  private val loginAdminSuperset = new LoginInfo(ConfigReader.suspersetAdminUser, ConfigReader.suspersetAdminPwd, "superset")


  private def handleServiceCall[A](serviceInvoke:(String,WSClient)=> Future[WSResponse], handleJson:(JsValue)=> Either[Error, A] )={

    secInvokeManager.manageRestServiceCall(loginAdminSuperset, serviceInvoke,200,500).map {
      case Right(json) => handleJson(json)
      case Left(l) =>  Left( Error(Option(0),Some(l),None) )
    }
  }


  def createDatabase(dataSource:String, userName:String, userPwd:String, connectedDbName:String ): Future[Either[Error, Success]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val stringRequest = ConfigReader.suspersetDbUri.startsWith("impala") match {

        case true => s"""{
                          "database_name": "$dataSource",
                          "extra":"${StringEscapeUtils.escapeJson("""{ "metadata_params": {}, "engine_params": { "connect_args": {"use_ssl":"true"}} }""")}",
                          "sqlalchemy_uri": "${ConfigReader.suspersetDbUri}/$connectedDbName?auth_mechanism=PLAIN&password=$userPwd&user=$userName",
                          "impersonate_user": "false"
                          }"""
        // for testing pourpose
        case false => s"""{
                          "database_name": "$dataSource",
                          "extra":"${StringEscapeUtils.escapeJson("""{ "metadata_params": {},"engine_params": {} }""")}",
                          "sqlalchemy_uri": "${ConfigReader.suspersetDbUri}",
                          "impersonate_user": "false"
                          }"""

      }


      val jsonRequest: JsValue = Json.parse(stringRequest)

      Logger.logger.debug("createSuspersetDatabase request: " + jsonRequest.toString())

      wSClient.url(ConfigReader.supersetUrl + "/databaseview/api/create").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).post(jsonRequest)
    }

    def handleJson(json:JsValue)={

      ((json \ "item") \ "perm").validate[String] match {
        case s: JsSuccess[String] => s.asOpt match {
          case None | Some("None") | Some("") => Left(Error(Option(0), Some("Error in createSuspersetDatabase"), None))
          case _ => Right(Success(Some("Connection created"), Some("ok")))
        }
        case e: JsError => Left(Error(Option(0), Some("Error in createSuspersetDatabase"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }



  def findRoleIds(roleNames: String*): Future[Either[Error, List[Long]]] = {

    val traversed = roleNames.toList.traverse[Future, Either[Error, Long]](findRoleId): Future[List[Either[Error, Long]]]

    traversed.map { lista =>
      val out = lista.foldLeft(List[Long]())((a, b) => b match {
        case Right(r) => r :: a
        case _ => a
      })
      Right(out)
    }

  }


  def findRoleId(roleName: String): Future[Either[Error, Long]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/roles/api/read?_flt_1_name=$roleName").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    Logger.logger.debug("findSupersetRoleId roleName: " + roleName)

    def handleJson(json:JsValue)={

      (json \ "pks") (0).validate[Long] match {
        case s: JsSuccess[Long] => Right(s.value)
        case e: JsError => Left(Error(Option(0), Some("Error in findSupersetRoleId"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }


  private def findRoleWithPermissions(roleName: String, permissionMap:Map[String,Long]): Future[Either[Error, SupersetRoleWithPermissionIds]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/roles/api/read?_flt_1_name=$roleName").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    Logger.logger.debug("findRole roleName: " + roleName)


    def handleJson(json:JsValue)=
      json.validate[SupersetRoleWithPermissionNames] match {
        case s: JsSuccess[SupersetRoleWithPermissionNames] => Right(SupersetRoleWithPermissionIds(s.value.role, s.value.permissions.map(permissionMap(_)).toList))
        case e: JsError => Left(Error(Option(0), Some("Error in findRole"), None))
      }



    handleServiceCall(serviceInvoke,handleJson)


  }


  private def findUserIdsOfRole(roleId: Long): Future[Either[Error, Seq[Long]]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {
      wSClient.url(ConfigReader.supersetUrl + s"/users/api/read?_flt_0_roles=$roleId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    Logger.logger.debug("findUserIdsOfRole roleId: " + roleId)


    def handleJson(json:JsValue)={

      (json \ "pks").validate[Seq[Long]] match {
        case s: JsSuccess[Seq[Long]] => Right(s.value)
        case e: JsError => Left(Error(Option(0), Some("Error in findSupersetRoleId"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)

  }

  private def getPermissionMap(): Future[Either[Error, Map[String,Long]]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/permissionviews/api/readvalues").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    Logger.logger.debug("listPermission")


    def handleJson(json:JsValue)= json.validate[Seq[JsObject]] match {
      case JsSuccess(value,pth) => Right(value.map( elem => (((elem \ "text").as[String]),((elem \ "id").as[Long]))).toMap)
      case JsError(e) => Left(Error(Option(0), Some(s"Error in listPermission: $e"), None))
    }

    handleServiceCall(serviceInvoke,handleJson)

  }


  def addTablePermissionToRole(dbName:String,schemaName:Option[String],tableName:String,roleName:String):Future[Either[Error, Success]]={

    val result = for{

      dbId <- EitherT(findDatabaseId(dbName))
      tableId <- EitherT(findTableId(dbId,schemaName,tableName))

      out <- EitherT(addPermissionsToRole(List(toPermissionName(Some(dbName),tableName,tableId)),roleName))
    } yield out

    result.value
  }

  def removeTablePermissionFromRole(dbName:String,schemaName:Option[String],tableName:String,roleName:String):Future[Either[Error, Success]]={

    val result = for{

      dbId <- EitherT(findDatabaseId(dbName))
      tableId <- EitherT(findTableId(dbId,schemaName,tableName))

      out <- EitherT(removePermissionsFromRole(List(toPermissionName(Some(dbName),tableName,tableId)),roleName))
    } yield out

    result.value
  }

  def addTablesPermissionToRole(schemaName:Option[String],tableName:String,roleName:String):Future[Either[Error, Success]]={

    val result = for{
      tablesId <- EitherT(findTablesId(schemaName,tableName))
      out <- EitherT(addPermissionsToRole(tablesId.toList.map(toPermissionName(None,tableName,_)),roleName))
    } yield out

    result.value
  }

  def removeTablesPermissionFromRole(schemaName:Option[String],tableName:String,roleName:String):Future[Either[Error, Success]]={

    val result = for{
      tablesId <- EitherT(findTablesId(schemaName,tableName))
      out <- EitherT(removePermissionsFromRole(tablesId.toList.map(toPermissionName(None,tableName,_)),roleName))
    } yield out

    result.value
  }


  private def toPermissionName(dbName:Option[String],tableName:String,tableId:Long) = {

    val prefix = dbName match{
      case Some(x) => s"datasource access on [$x]"
      case None => ""
    }

    s"$prefix.[$tableName](id:$tableId)"

  }

  private def getIdsFromMap( permissionNames:List[String], permissionMap:Map[String,Long] ):List[Long]={

    permissionNames.map{ elem =>

      if(elem.startsWith("datasource access")) permissionMap(elem)
      else{
        val filteredMap = permissionMap.filter(_._1.endsWith(elem))

        if(filteredMap.size>1)
          Logger.logger.error("getIdsFromMap return more than one element")

        filteredMap.values.head
      }
    }

  }


  private def addPermissionsToRole(permissionsName:List[String],roleName:String):Future[Either[Error, Success]]={

    val result = for{

      permissionMap <- EitherT(getPermissionMap())
      supersetRoleInfo <- EitherT(findRoleWithPermissions(roleName,permissionMap))
      userList <- EitherT(findUserIdsOfRole(supersetRoleInfo.role.id))
      out <- EitherT(updateRole(SupersetRoleWithUsersAndPermissionIds(supersetRoleInfo.role,getIdsFromMap(permissionsName,permissionMap):::supersetRoleInfo.permissions,userList)))
    } yield out

    result.value
  }

  private def removePermissionsFromRole(permissionsName:List[String],roleName:String):Future[Either[Error, Success]]={

    val result = for{

      permissionMap <- EitherT(getPermissionMap())
      supersetRoleInfo <- EitherT(findRoleWithPermissions(roleName,permissionMap))
      userList <- EitherT(findUserIdsOfRole(supersetRoleInfo.role.id))
      //newPermissions <-EitherT(removePermission(permissionsId,supersetRoleInfo.permissions))
      out <- EitherT(updateRole(SupersetRoleWithUsersAndPermissionIds(supersetRoleInfo.role,supersetRoleInfo.permissions diff getIdsFromMap(permissionsName,permissionMap),userList)))
    } yield out

    result.value
  }

/*
  private def removePermission(permsId:List[Long], permissionList:List[Long]):Future[Either[Error, List[Long]]] = {

    Future.successful{
      if(!permissionList.contains(permId))
        Left(Error(Option(0), Some(s"Permission $permId not present in role"), None))
      else
        Right( permissionList.filterNot(_ == permId) )
    }

  }*/

  /*
  private def findSupersetRoleWithUsers(roleName:String): Future[Either[Error, SupersetRoleWithUsersAndPermissionIds]] ={

    val result = for{
      permissionMap <- EitherT(getPermissionMap())
      supersetRoleInfo <- EitherT(findRole(roleName,permissionMap))
      userList <- EitherT(findUserIdsOfRole(supersetRoleInfo.role.id))
    }yield SupersetRoleWithUsersAndPermissionIds(supersetRoleInfo.role,supersetRoleInfo.permissions,userList)

    result.value
  }*/


  def createUserWithRoles(ipaUser: IpaUser, roleIds: Long*): Future[Either[Error, Success]] = {

    val roleIdsJsonString = roleIds.mkString("[\"", "\",\"", "\"]")

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val jsonRequest: JsValue = Json.parse(
        s"""{
                                                "active": true,
                                                "email": "${ipaUser.mail}",
                                                "first_name": "${ipaUser.givenname}",
                                                "last_name": "${ipaUser.sn}",
                                                "username": "${ipaUser.uid}",
                                                "roles": $roleIdsJsonString
                                                }""")


      Logger.logger.debug("createSupersetUserWithRole request: " + jsonRequest.toString())

      wSClient.url(ConfigReader.supersetUrl + "/users/api/create").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).post(jsonRequest)
    }


    def handleJson(json:JsValue):Either[Error, Success]={

      ((json \ "item") \ "username").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("Connection created"), Some("ok")))
        case e: JsError => Left(Error(Option(0), Some("Error in createSupersetUserWithRole"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }


  def findDatabaseId(dbName: String): Future[Either[Error, Long]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/databaseview/api/read?_flt_3_database_name=$dbName").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    Logger.logger.debug("findSupersetDatabaseId dbName: " + dbName)

    def handleJson(json:JsValue)={

      (json \ "pks") (0).validate[Long] match {
        case s: JsSuccess[Long] => Right(s.value)
        case e: JsError => Left(Error(Option(0), Some("Error in findSupersetDatabaseId"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)

  }


  type SupersetUserInfo = (Long, Array[String])

  def findUser(username: String): Future[Either[Error, SupersetUserInfo]] = {


    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/users/api/read?_flt_1_username=$username").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    Logger.logger.debug("findUser username: " + username)

    def handleJson(json:JsValue)={

      (json \ "pks") (0).validate[Long] match {
        case s: JsSuccess[Long] => ((json \ "result") (0) \ "roles").validate[Array[String]] match {
          case s2: JsSuccess[Array[String]] => Right((s.value, s2.value))
          case e2: JsError => println("Error response: " + json); Left(Error(Option(0), Some("Error in findSupersetUserId"), None))
        }
        case e: JsError => Left(Error(Option(0), Some("Error in findSupersetUserId"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }


  def deleteUser(userId: Long): Future[Either[Error, Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      Logger.logger.debug("deleteSupersetUser userId: " + userId)

      wSClient.url(ConfigReader.supersetUrl + s"/users/api/delete/$userId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    Logger.logger.debug("deleteSupersetUser userId: " + userId)

    def handleJson(json:JsValue)={

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("User deleted"), Some("ok")))
        case e: JsError => Left(Error(Option(0), Some("Error in deleteSupersetUser"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }


  def deleteRole(roleId: Long): Future[Either[Error, Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      Logger.logger.debug("deleteSupersetRole roleId: " + roleId)

      wSClient.url(ConfigReader.supersetUrl + s"/roles/api/delete/$roleId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    Logger.logger.debug("deleteSupersetRole roleId: " + roleId)

    def handleJson(json:JsValue)={

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("Role deleted"), Some("ok")))
        case e: JsError => Left(Error(Option(0), Some("Error in deleteSupersetRole"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }

  def deleteDatabase(dbId: Long): Future[Either[Error, Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      Logger.logger.debug("deleteSupersetDatabase dbId: " + dbId)

      wSClient.url(ConfigReader.supersetUrl + s"/databaseview/api/delete/$dbId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    Logger.logger.debug("deleteSupersetDatabase dbId: " + dbId)

    def handleJson(json:JsValue)={

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("Db deleted"), Some("ok")))
        case e: JsError => Left(Error(Option(0), Some("Error in deleteSupersetDatabase"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }


  def checkDbTables(dbId:Long):Future[Either[Error,Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/tablemodelview/api/readvalues?_flt_0_database=$dbId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    Logger.logger.debug("checkDbTables id: " + dbId)


    def handleJson(json:JsValue)={

      json(0).validate[JsValue] match {
        case s: JsSuccess[JsValue] =>  Left(Error(Option(1), Some("Some tables on Superset founded. Please delete them before cancel datasource"), None))
        case e: JsError =>  Right(Success(Some("Tables not presents"), Some("ok")))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)

  }


  def findTableSlices(tableId:Long):Future[Either[Error,Success]]={

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/slicemodelview/api/read").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    Logger.logger.debug("findTableSlices tableId: " + tableId)


    def handleJson(json:JsValue)={

      (json \ "result").validate[Seq[JsObject]] match {

        case JsSuccess(value,pth) =>  val test = value.forall{ elem =>
                                                              val link = (elem \ "datasource_link").asOpt[String].getOrElse("")
                                                              !link.contains(s"""/$tableId/""")
                                                            }
                                      if(test) Right(Success(Some("ok"), Some("ok")))
                                      else Left(Error(Option(1), Some("Table have attacched slices"), None))

        case JsError(e) => Left(Error(Option(0), Some("Error in findTableSlices"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)

  }


  def findDbTables(dbId:Long):Future[Either[Error,Seq[(Long,String)]]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/tablemodelview/api/readvalues?_flt_0_database=$dbId").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    Logger.logger.debug("findDbTables id: " + dbId)


    def handleJson(json:JsValue)={

      json.validate[Seq[JsObject]] match {
        case JsSuccess(value,pth) => Right( value.map{ elem => ((elem \ "id").as[Long],(elem \ "text").as[String])} )
        case JsError(e) => Right(Seq())
      }
    }

    handleServiceCall(serviceInvoke,handleJson)

  }



  def findTableId(dbId:Long, schema:Option[String], tableName:String):Future[Either[Error,Long]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val schemaParam= if(schema.nonEmpty) s"&_flt_3_schema=${schema.get}" else ""
      val url = ConfigReader.supersetUrl + s"/tablemodelview/api/readvalues?_flt_0_database=$dbId&_flt_3_table_name=$tableName$schemaParam"

      Logger.logger.debug("findTableId url: " + url)

      wSClient.url(url).withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    def handleJson(json:JsValue)={

      (json(0) \ "id").validate[Long] match {
        case s: JsSuccess[Long] =>  Right(s.value)
        case e: JsError =>  Left(Error(Option(1), Some("Table does not exists"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)

  }

  def findTablesId(schema:Option[String], tableName:String):Future[Either[Error,Seq[Long]]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      val schemaParam= if(schema.nonEmpty) s"&_flt_3_schema=${schema.get}" else ""
      val url = ConfigReader.supersetUrl + s"/tablemodelview/api/readvalues?_flt_3_table_name=$tableName$schemaParam"

      Logger.logger.debug("findTablesId url: " + url)

      wSClient.url(url).withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    def handleJson(json:JsValue)={

      json.validate[Seq[JsObject]] match {
        case JsSuccess(value,pth) => Right( value.map{ elem => (elem \ "id").as[Long]} )
        case JsError(e) => Right(Seq())
      }
    }

    handleServiceCall(serviceInvoke,handleJson)

  }


  def deleteTable(id: Long): Future[Either[Error, Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      Logger.logger.debug("deleteTable id: " + id)

      wSClient.url(ConfigReader.supersetUrl + s"/tablemodelview/api/delete/$id").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    Logger.logger.debug("deleteTable id: " + id)

    def handleJson(json:JsValue)={

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("table deleted"), Some("ok")))
        case e: JsError => Left(Error(Option(0), Some("Error in deleteTable"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }


  // TODO not a service for now, due to Superset issue
  def createTable(dbId:Long, schema:Option[String], tableName:String): Future[Either[Error, Success]] = {

    val postData = s"database=$dbId${if(schema.nonEmpty) "&schema="+schema.get else ""}&table_name=$tableName"

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      Logger.logger.debug("createTable request: " + postData)


      wSClient.url(ConfigReader.supersetUrl + "/tablemodelview/add").withHeaders(
        "Content-Type" -> """application/x-www-form-urlencoded""",
        "Accept-Encoding" -> "gzip, deflate, br",
        "Accept-Language" -> "en-US,en;q=0.5",
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "User-Agent" -> """Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:58.0) Gecko/20100101 Firefox/58.0""",
        //"Host" -> ConfigReader.supersetUrl,
        //"Referer" -> (ConfigReader.supersetUrl + "/tablemodelview/add"),
        "Content-Length" -> postData.length.toString,
        "Connection" -> "keep-alive",
        "Upgrade-Insecure-Requests" -> "1",
        "Cookie" -> sessionCookie
      ).withFollowRedirects(false).post(postData)
    }


    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map{ response=>
      if(response.status == 302)
        Right(Success(Some("Table created"), Some("ok")))
      else
        Left(Error(Option(0), Some("Error in create superset table"), None))
    }

  }


  // TODO not a service for now, due to Superset issue
  def updateUser(user:IpaUser, userId:Long, roles:List[Long]): Future[Either[Error, Success]] = {

    val postDataRoles = roles.foldLeft("")( (a,b)=>a+"&roles="+b )
    val postData = s"first_name=${user.givenname}&last_name=${user.sn}&username=${user.uid}&active=y&email=${user.mail}$postDataRoles"

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      Logger.logger.debug("updateUser request: " + postData)


      wSClient.url(ConfigReader.supersetUrl + s"/users/edit/$userId").withHeaders(
        "Content-Type" -> """application/x-www-form-urlencoded""",
        "Accept-Encoding" -> "gzip, deflate, br",
        "Accept-Language" -> "en-US,en;q=0.5",
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "User-Agent" -> """Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:58.0) Gecko/20100101 Firefox/58.0""",
        "Content-Length" -> postData.length.toString,
        "Connection" -> "keep-alive",
        "Upgrade-Insecure-Requests" -> "1",
        "Cookie" -> sessionCookie
      ).withFollowRedirects(false).post(postData)
    }


    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map{ response=>
      if(response.status == 302)
        Right(Success(Some("User edited"), Some("ok")))
      else
        Left(Error(Option(0), Some("Error in update superset user"), None))
    }

  }

  // TODO not a service for now, due to Superset issue
  private def updateRole(supersetRole:SupersetRoleWithUsersAndPermissionIds): Future[Either[Error, Success]] = {

    val postDataPermission = supersetRole.permissions.foldLeft("")( (a,b)=>a+"&permissions="+b )
    val postDataUser = supersetRole.users.foldLeft("")( (a,b)=>a+"&user="+b )
    val postData = s"name=${supersetRole.role.name}$postDataPermission$postDataUser"

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      Logger.logger.debug("updateRole request: " + postData)


      wSClient.url(ConfigReader.supersetUrl + s"/roles/edit/${supersetRole.role.id}").withHeaders(
        "Content-Type" -> """application/x-www-form-urlencoded""",
        "Accept-Encoding" -> "gzip, deflate, br",
        "Accept-Language" -> "en-US,en;q=0.5",
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "User-Agent" -> """Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:58.0) Gecko/20100101 Firefox/58.0""",
        "Content-Length" -> postData.length.toString,
        "Connection" -> "keep-alive",
        "Upgrade-Insecure-Requests" -> "1",
        "Cookie" -> sessionCookie
      ).withFollowRedirects(false).post(postData)
    }


    secInvokeManager.manageServiceCall(loginAdminSuperset, serviceInvoke).map{ response=>
      if(response.status == 302)
        Right(Success(Some("Role edited"), Some("ok")))
      else
        Left(Error(Option(0), Some("Error in update superset role"), None))
    }

  }

  def findPermissionViewIds(view_menu_id:Long):Future[Either[Error,Array[Option[Long]]]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/permissionviews/api/readvalues?_flt_0_view_menu_id=$view_menu_id").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }


    Logger.logger.debug("findPermissionViewIds view_menu_id: " + view_menu_id)

    def handleJson(json:JsValue)={

      json.validate[Array[JsValue]] match {
        case s: JsSuccess[Array[JsValue]] =>  Right(
          s.value.map{ jsval =>
            (jsval \ "id").validate[Long] match {
              case si: JsSuccess[Long] => Some(si.value)
              case ei: JsError =>  None
            }
          }
        )
        case e: JsError => Left(Error(Option(0), Some("Error in findPermissionViewIds"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }


  def findViewId(permName:String):Future[Either[Error,Long]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      wSClient.url(ConfigReader.supersetUrl + s"/viewmenus/api/read?_flt_3_name=$permName").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).get
    }

    Logger.logger.debug("findViewId permName: " + permName)

    def handleJson(json:JsValue):Either[Error,Long]={

      (json \ "pks") (0).validate[Long] match {
        case s: JsSuccess[Long] => Right(s.value)
        case e: JsError => Left(Error(Option(0), Some("Error in findViewId"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }


  def deleteView(id: Long): Future[Either[Error, Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      Logger.logger.debug("deleteView id: " + id)

      wSClient.url(ConfigReader.supersetUrl + s"/viewmenus/api/delete/$id").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    Logger.logger.debug("deleteSupersetDatabase id: " + id)

    def handleJson(json:JsValue)={

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("view deleted"), Some("ok")))
        case e: JsError => Left(Error(Option(0), Some("Error in deleteView"), None))
      }
    }

    handleServiceCall(serviceInvoke,handleJson)


  }


  def deletePermissionsViews(ids: Array[Option[Long]]): Future[Either[Error, Success]] = {

    val ok: Future[Either[Error, Success]]  = Future{Right(Success(Some("permission-view deleted"), Some("ok")))}
    val ko: Future[Either[Error, Success]]  = Future{Left(Error(Option(0), Some("Error in deletePermissionView"), None))}


    ids.foldLeft(ok)((b,a)=>{
      b.flatMap {
        case Right(r) => deletePermissionView(a)
        case Left(l) => ko
      }
    })


  }


  private def deletePermissionView(id: Option[Long]): Future[Either[Error, Success]] = {

    id match{
      case Some(x) => deletePermissionView(x)
      case None => Future{Left(Error(Option(0), Some("Error in deletePermissionView"), None))}
    }

  }


  private def deletePermissionView(id: Long): Future[Either[Error, Success]] = {

    def serviceInvoke(sessionCookie: String, wSClient: WSClient): Future[WSResponse] = {

      Logger.logger.debug("deleteView id: " + id)

      wSClient.url(ConfigReader.supersetUrl + s"/permissionviews/api/delete/$id").withHeaders("Content-Type" -> "application/json",
        "Accept" -> "application/json",
        "Cookie" -> sessionCookie
      ).delete()
    }

    Logger.logger.debug("deletePermissionView id: " + id)

    def handleJson(json:JsValue)={

      (json \ "message").validate[String] match {
        case s: JsSuccess[String] => Right(Success(Some("permission-view deleted"), Some("ok")))
        case e: JsError => Left(Error(Option(0), Some("Error in deletePermissionView"), None))
      }

    }

    handleServiceCall(serviceInvoke,handleJson)


  }

  implicit val supersetRole: Reads[SupersetRole] = (
      ((JsPath \ "result") (0)\ "name").read[String] and
      (JsPath \ "pks") (0).read[Long]
    )(SupersetRole.apply _)

  implicit val supersetRoleWithPermissionNames: Reads[SupersetRoleWithPermissionNames] = (
      JsPath.read[SupersetRole] and
      ((JsPath \ "result") (0)\ "permissions").read[Seq[String]]
    )(SupersetRoleWithPermissionNames.apply _)

  case class SupersetRole(name:String, id:Long)
  case class SupersetRoleWithPermissionNames(role:SupersetRole, permissions:Seq[String])
  case class SupersetRoleWithPermissionIds(role:SupersetRole, permissions:List[Long])
  case class SupersetRoleWithUsersAndPermissionIds(role:SupersetRole,permissions:List[Long],users:Seq[Long])

}




