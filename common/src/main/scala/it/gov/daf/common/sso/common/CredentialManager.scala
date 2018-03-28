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

import java.util

import com.google.inject.{Inject, Singleton}
import it.gov.daf.common.authentication.{Authentication, Role}
import it.gov.daf.common.utils._
import org.apache.commons.net.util.Base64
import play.api.Logger
import play.api.mvc.{Request, RequestHeader}

import scala.util.Try

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.ToString",
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.OptionPartial",
    "org.wartremover.warts.AsInstanceOf"
  )
)
@Singleton
class CredentialManager @Inject()(cacheWrapper: CacheWrapper) {

  def readBaCredentials( requestHeader:RequestHeader):UserInfoSearch= {

    val authHeader = requestHeader.headers.get("authorization").get.split(" ")
    val authType = authHeader(0)
    val authCrendentials = authHeader(1)


    if( authType.equalsIgnoreCase("basic") ){

      val pwd:String = new String(Base64.decodeBase64(authCrendentials.getBytes)).split(":")(1)
      val user:String= Authentication.getProfiles(requestHeader).head.getId
      val ldapGroups = Authentication.getProfiles(requestHeader).head.getAttribute("memberOf").asInstanceOf[util.Collection[String]].toArray()
      val groups: Array[String] = ldapGroups.map( _.toString().split(",")(0).split("=")(1) )

      Credentials(user, pwd, groups)
    }else
      Empty()

  }


  def readBearerCredentials(requestHeader: RequestHeader):UserInfoSearch= {

    val authHeader = requestHeader.headers.get("authorization").get.split(" ")
    val authType = authHeader(0)

    if( authType.equalsIgnoreCase("bearer") ) {

      //Logger.logger.debug(s"claims:${Authentication.getClaims(requestHeader)}")

      val claims = Authentication.getClaims(requestHeader).get
      val ldapGroups = claims.get("memberOf").asInstanceOf[Option[Any]].get.asInstanceOf[net.minidev.json.JSONArray].toArray
      val groups: Array[String] = ldapGroups.map(_.toString.split(",")(0).split("=")(1))
      val user: String = claims("sub").toString

      //Logger.logger.info(s"JWT user: $user")
      //Logger.logger.info(s"belonging to groups: ${groups.toList}" )

      Profile(user, groups)
    }else
      Empty()

  }

  def readCredentialFromRequest( requestHeader: RequestHeader ):UserInfo = {

    readBearerCredentials(requestHeader) match {
      case p:Profile => p
      case e:Empty => readBaCredentials(requestHeader) match{
        case c:Credentials => c
        case _ => throw new Exception("Authorization header not found")
      }
    }

  }

  def tryToReadCredentialFromRequest( requestHeader: RequestHeader ):Try[UserInfo] = {
    Try{readCredentialFromRequest(requestHeader)}
  }

  def getCredentials( requestHeader: RequestHeader ):Try[Credentials] = {

    Try{
      readCredentialFromRequest(requestHeader) match {
        case p:Profile => cacheWrapper.getPwd(p.username) match{
          case Some(pwd) => Credentials(p.username, pwd, p.groups)
          case None => throw new Exception("Can't find credentails in cache")
        }
        case c:Credentials => c
      }
    }

  }


  def isDafAdmin(request:Request[Any]):Boolean ={
    val groups = readCredentialFromRequest(request).groups
    Logger.logger.info(s"belonging to groups: ${groups.toList}" )
    groups.contains(Role.Admin.toString)
  }

  def isDafEditor(request:Request[Any]):Boolean ={
    val groups = readCredentialFromRequest(request).groups
    Logger.logger.info(s"belonging to groups: ${groups.toList}" )
    groups.contains(Role.Editor.toString)
  }


  def isBelongingToGroup( request:Request[Any], group:String ):Boolean ={
    val groups = readCredentialFromRequest(request).groups
    Logger.logger.info(s"belonging to groups: ${groups.toList}" )
    groups.contains(group)
  }

}
