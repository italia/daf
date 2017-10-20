package it.gov.daf.sso.common

import com.google.common.cache.CacheBuilder
import scala.concurrent.duration._
import scalacache._
import scalacache.guava._

class CacheWrapper(cookieTtlMin:Long, credentialTtlMin:Long) {

  private val guavaCache = CacheBuilder.newBuilder().maximumSize(10000L).build[String, Object]
  implicit val cache = ScalaCache(GuavaCache(guavaCache))

  private def get(key:String):Option[String] = sync.get(key)
  private def delete(key:String) = remove(key)
  private def put(key:String,value:String,d:Duration) = sync.cachingWithTTL(key)(d){value}


  def getCookie(appName:String,userName:String):Option[String] = get(appName+"-"+userName)
  def getPwd(user:String):Option[String] = get(user)
  def putCookie(appName:String,userName:String,value:String) = put(appName+"-"+userName,value, cookieTtlMin.minutes)
  def putCredentials(user:String,pwd:String) = put(user,pwd, credentialTtlMin.minutes) //TTL must be equal to jwt expiration
  def deleteCookie(appName:String,userName:String)= delete(appName+"-"+userName)
  def deleteCredentials(user:String) = delete(user)


}

object CacheWrapper{

  private var _instance : CacheWrapper = null

  def init(cookieTtlMin:Long, credentialTtlMin:Long) = {
    if (_instance == null)
      _instance = new CacheWrapper(cookieTtlMin,credentialTtlMin)
    _instance
  }

  def instance={
    if(_instance==null)
      throw new Exception("CacheWrapper not initailized")
    else
      _instance
  }


}