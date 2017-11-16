/*
 * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.gov.daf.common.sso.common

import com.google.common.cache.CacheBuilder
import play.api.mvc.Cookie

import scala.concurrent.duration._
import scalacache._
import scalacache.guava._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.PublicInference"
  )
)
class CacheWrapper(_cookieTtlMin: Long, _credentialTtlMin: Long) {

  val cookieTtlMin = _cookieTtlMin
  val credentialTtlMin = _credentialTtlMin
  private val guavaCache = CacheBuilder.newBuilder().maximumSize(10000L).build[String, Object]
  private implicit val cache = ScalaCache(GuavaCache(guavaCache))

  //private def get(key:String):Option[String] = sync.get(key)
  //private def delete(key:String) = remove(key)
  //private def put(key:String,value:String,d:Duration) = sync.cachingWithTTL(key)(d){value}

  def getCookie(appName: String, userName: String): Option[Cookie] = sync.get(appName + "-" + userName)
  def getCookies(appName: String, userName: String): Option[Seq[Cookie]] = sync.get(appName + "-" + userName + "multi")
  def getPwd(user: String): Option[String] = sync.get(user)
  def putCookie(appName: String, userName: String, cookie: Cookie) = sync.cachingWithTTL(appName + "-" + userName)(cookieTtlMin.minutes) { cookie }
  def putCookies(appName: String, userName: String, cookies: Seq[Cookie]) = sync.cachingWithTTL(appName + "-" + userName + "multi")(cookieTtlMin.minutes) { cookies }
  def putCredentials(user: String, pwd: String) = sync.cachingWithTTL(user)(credentialTtlMin.minutes) { pwd } //TTL must be equal to jwt expiration
  def deleteCookie(appName: String, userName: String) = remove(appName + "-" + userName)
  def deleteCookies(appName: String, userName: String) = remove(appName + "-" + userName + "multi")
  def deleteCredentials(user: String) = remove(user)

}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.Null"
  )
)
object CacheWrapper {

  private var _instance: CacheWrapper = null

  def init(cookieTtlMin: Long, credentialTtlMin: Long): CacheWrapper = {
    if (_instance == null) {
      _instance = new CacheWrapper(cookieTtlMin, credentialTtlMin)
      _instance
    } else if (cookieTtlMin == _instance.cookieTtlMin && credentialTtlMin == _instance.credentialTtlMin)
      _instance
    else
      throw new Exception("CacheWrapper is already initailized with different parameters")
  }

  def instance: CacheWrapper = {
    if (_instance == null)
      throw new Exception("CacheWrapper not initailized")
    else
      _instance
  }

  def isInitialized: Boolean = (_instance != null)

}