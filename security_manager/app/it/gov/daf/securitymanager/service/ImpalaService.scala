package it.gov.daf.securitymanager.service

import com.cloudera.impala.jdbc41.DataSource
import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.sso.common.CacheWrapper
import it.gov.daf.securitymanager.service.utilities.ConfigReader


@Singleton
class ImpalaService @Inject()(implicit val cacheWrapper:CacheWrapper){

  private val ds:DataSource = new DataSource()
  ds.setURL(s"jdbc:impala://${ConfigReader.impalaServer};SSL=1;SSLKeyStore=${ConfigReader.impalaKeyStorePath};SSLKeyStorePwd=${ConfigReader.impalaKeyStorePwd};CAIssuedCertNamesMismatch=1;AuthMech=3")


  def createSelectGrant(tableName:String, groupName:String):Boolean= createGrant(tableName,groupName,"SELECT")

  def createInsertGrant(tableName:String, groupName:String):Boolean= createGrant(tableName,groupName,"INSERT")

  private def createGrant(tableName:String, groupName:String, permission:String):Boolean={
    val roleName = s"${groupName}_group_role"
    val query = s"GRANT $permission ON TABLE $tableName TO ROLE $roleName;"

    executeUpdate(query)>0
  }


  def revokeSelectGrant(tableName:String, groupName:String):Boolean= createGrant(tableName,groupName,"SELECT")

  def revokeInsertGrant(tableName:String, groupName:String):Boolean = createGrant(tableName,groupName,"INSERT")

  private def revokeGrant(tableName:String, groupName:String, permission:String):Boolean={

    val roleName = s"${groupName}_group_role"
    val query = s"REVOKE $permission ON TABLE $tableName FROM ROLE $roleName;"

    executeUpdate(query)>0
  }


  private def executeUpdate(query:String):Int={

    val loginInfo = readLoginInfo

    val conn = ds.getConnection(loginInfo.user,loginInfo.password)
    val stmt = conn.createStatement()
    val res = stmt.executeUpdate(query)

    conn.close()

    res

  }

  private def executeQuery(user:String,pwd:String, query:String):String={


    val conn = ds.getConnection(user,pwd)

    val stmt = conn.createStatement()
    val rs = stmt.executeQuery("SHOW ROLE GRANT GROUP default_org;")

    val res = if( rs.next() ) rs.getString(1)
              else "not found"

    //println("weeee-->"+rs.getString(1))

    conn.close()

    res

  }

}
