package it.gov.daf.securitymanager.service

import com.cloudera.impala.jdbc41.DataSource
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.CacheWrapper
import it.gov.daf.securitymanager.service.utilities.ConfigReader


@Singleton
class ImpalaService @Inject()(implicit val cacheWrapper:CacheWrapper){

  private val ds:DataSource = new DataSource()
  ds.setURL(s"jdbc:impala://${ConfigReader.impalaServer};SSL=1;SSLKeyStore=${ConfigReader.impalaKeyStorePath};SSLKeyStorePwd=${ConfigReader.impalaKeyStorePwd};CAIssuedCertNamesMismatch=1;AuthMech=3")


  def createGrant(tableName:String, groupName:String, permission:String):Either[String,String]={

    val permissionOnQuery = if(permission == Permission.read.toString) "SELECT"
                            else "INSERT"


    val roleName = s"${groupName}_group_role"
    val query = s"GRANT $permissionOnQuery ON TABLE $tableName TO ROLE $roleName;"

    if(executeUpdate(query)>0) Right("Grant created")
    else Left("Can not create Impala grant")
  }


  def revokeGrant(tableName:String, groupName:String, permission:String):Either[String,String]={

    val permissionOnQuery = if(permission == Permission.read.toString) "SELECT"
    else "INSERT"

    val roleName = s"${groupName}_group_role"
    val query = s"REVOKE $permissionOnQuery ON TABLE $tableName FROM ROLE $roleName;"

    if(executeUpdate(query)>0) Right("Grant revoked")
    else Left("Can not revoke Impala grant")
  }


  def createGroupRole(groupName:String):Either[String,String]={

    val roleName = s"${groupName}_group_role"
    val query = s"CREATE ROLE $roleName;"

    if(executeUpdate(query)>0) Right("Group created")
    else Left("Can not create Impala group")
  }


  private def executeUpdate(query:String):Int={

    val loginInfo = readLoginInfo

    val conn = ds.getConnection(loginInfo.user,loginInfo.password)
    val stmt = conn.createStatement()
    val res = stmt.executeUpdate(query)

    conn.close()
    res

  }


  // for testing pourpose
  private def executeQuery(query:String):scala.collection.mutable.Map[String,String] ={

    val loginInfo = readLoginInfo
    val conn = ds.getConnection(loginInfo.user,loginInfo.password)

    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(query) //for example "SHOW ROLE GRANT GROUP default_org;"

    val metadata = rs.getMetaData
    val columns = metadata.getColumnCount
    var resultMap = scala.collection.mutable.Map[String,String]()
    while( rs.next() ) {
      for(i <- 0 to columns-1)
        resultMap += metadata.getColumnName(i) -> rs.getString(i)
    }

    conn.close()
    resultMap

  }

}
