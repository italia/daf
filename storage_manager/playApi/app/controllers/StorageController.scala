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

package controllers

import java.lang.reflect.UndeclaredThrowableException
import java.nio.file.InvalidPathException
import java.security.PrivilegedExceptionAction
import it.gov.daf.common.authentication.Authentication
import it.teamdigitale.PhysicalDatasetController
import org.apache.spark.sql.types.{DataType, Metadata, StructField, StructType}
import org.apache.spark.sql.{AnalysisException, SparkSession}
import org.pac4j.play.store.PlaySessionStore
import com.google.inject.Inject
import io.swagger.annotations.{Api, ApiOperation, ApiParam, Authorization}
import org.apache.hadoop.security.{AccessControlException, UserGroupInformation}
import play.api.Configuration
import play.api.libs.json.{Json, OFormat, OWrites}
import play.api.mvc._
import play.mvc.Http
import play.Logger

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


@SuppressWarnings(
  Array(
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.IsInstanceOf",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.AsInstanceOf"
  )
)
@Api("physical-dataset")
class StorageController @Inject()(configuration: Configuration, val playSessionStore: PlaySessionStore) extends Controller {


  //
//
//  private val fileSystem: FileSystem = {
//    val conf = new org.apache.hadoop.conf.Configuration()
//    FileSystem.get(conf)
//  }
//
//  @tailrec
//  private def addClassPathJars(sparkContext: SparkContext, classLoader: ClassLoader): Unit = {
//    classLoader match {
//      case urlClassLoader: URLClassLoader =>
//        urlClassLoader.getURLs.foreach { classPathUrl =>
//          if (classPathUrl.toExternalForm.endsWith(".jar") && !classPathUrl.toExternalForm.contains("test-interface")) {
//            sparkContext.addJar(classPathUrl.toExternalForm)
//          }
//        }
//      case _ =>
//    }
//    if (classLoader.getParent != null) {
//      addClassPathJars(sparkContext, classLoader.getParent)
//    }
//  }

  UserGroupInformation.loginUserFromSubject(null)

  private val proxyUser = UserGroupInformation.getCurrentUser

  val storageManager = PhysicalDatasetController(configuration.underlying)

  Authentication(configuration, playSessionStore)

  private val exceptionManager: PartialFunction[Throwable, Result] = (exception: Throwable) => exception match {
    case ex: AnalysisException =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.NOT_FOUND, Map.empty))
    case ex: NotImplementedError =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.NOT_IMPLEMENTED, Map.empty))
    case ex: UndeclaredThrowableException if ex.getUndeclaredThrowable.isInstanceOf[AnalysisException] =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.NOT_FOUND, Map.empty))
    case ex: InvalidPathException =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.BAD_REQUEST, Map.empty))
    case ex: Throwable =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.INTERNAL_SERVER_ERROR, Map.empty))
  }

  private val hadoopExceptionManager: PartialFunction[Throwable, Result] = (exception: Throwable) => exception match {
    case ex: AccessControlException =>
      Ok(Json.toJson(ex.getMessage)).copy(header = ResponseHeader(Http.Status.UNAUTHORIZED, Map.empty))
  }

  private def CheckedAction(exceptionManager: Throwable => Result)(action: Request[AnyContent] => Result) = (request: Request[AnyContent]) => {
    Try(action(request)) match {
      case Success(response) => response
      case Failure(exception) => exceptionManager(exception)
    }
  }

  private def HadoopDoAsAction(action: Request[AnyContent] => Result) = (request: Request[AnyContent]) => {
    val profiles = Authentication.getProfiles(request)
    val user = profiles.headOption.map(_.getId).getOrElse("anonymous")
    val ugi = UserGroupInformation.createProxyUser(user, proxyUser)
    ugi.doAs(new PrivilegedExceptionAction[Result]() {
      override def run: Result = action(request)
    })
  }


  @ApiOperation(
    value = "dictionary of input parameters",
    produces = "application/json",
    authorizations = Array(new Authorization(value = "basicAuth"))
  )
  def getDataset(@ApiParam(value = "dictionary of input parameters", required = true) params: Map[String, String]): Action[AnyContent] =
    Action {
      CheckedAction(exceptionManager orElse hadoopExceptionManager) {
        HadoopDoAsAction {
          _ =>
            storageManager.get(params) match {
            case Success(df) =>
              val doc = s"[${df.collect().map(row => {Utility.rowToJson(df.schema)(row)}).mkString(",")}]"
              Ok(doc).as(JSON)
            case Failure(ex) =>
              Logger.error(s"Error for input params $params. ERROR message: ${ex.getMessage}")
              Ok(ex.getMessage).as(JSON)

          }
        }
      }
    }

  @ApiOperation(
    value = "dictionary of input parameters",
    produces = "application/json",
    authorizations = Array(new Authorization(value = "basicAuth"))
  )
   def getDatasetSchema(@ApiParam(value = "dictionary of input parameters", required = true) params: Map[String, String]): Action[AnyContent] =
    Action {
      CheckedAction(exceptionManager) {
        HadoopDoAsAction { _ =>
         val newParams = params + ("limit" -> 1)
          val res: Try[StructType] = storageManager.get(params).map(_.schema)

          res match {
            case Success(st) =>
              Ok(st.json)
            case Failure(ex) => Logger.error(s"Error for input params $params. ERROR message: ${ex.getMessage}")
              Ok(ex.getMessage).as(JSON)

          }
        }
      }
    }

}

