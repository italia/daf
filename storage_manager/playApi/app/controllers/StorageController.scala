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

///*
// * Copyright 2017 TEAM PER LA TRASFORMAZIONE DIGITALE
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers
//
//import java.lang.reflect.UndeclaredThrowableException
//import java.nio.file.InvalidPathException
//import java.security.PrivilegedExceptionAction
//
//import it.gov.daf.common.authentication.Authentication
//import it.teamdigitale.PhysicalDatasetController
//import org.apache.spark.sql.types.{DataType, Metadata, StructField, StructType}
//import org.apache.spark.sql.{AnalysisException, SparkSession}
//import org.pac4j.play.store.PlaySessionStore
//import com.google.inject.Inject
//import io.swagger.annotations.{Api, ApiOperation, ApiParam, Authorization}
//import org.apache.hadoop.security.{AccessControlException, UserGroupInformation}
//import play.api.Configuration
//import play.api.libs.json.{JsValue, Json, OFormat, OWrites}
//import play.api.mvc._
//import play.mvc.Http
//import play.Logger
//
//import scala.language.postfixOps
//import scala.util.{Failure, Success, Try}
//
//
//@SuppressWarnings(
//  Array(
//    "org.wartremover.warts.Throw",
//    "org.wartremover.warts.NonUnitStatements",
//    "org.wartremover.warts.Nothing",
//    "org.wartremover.warts.IsInstanceOf",
//    "org.wartremover.warts.Null",
//    "org.wartremover.warts.Var",
//    "org.wartremover.warts.AsInstanceOf"
//  )
//)
//@Api("physical-dataset")
//class StorageController @Inject()(configuration: Configuration, playSessionStore: PlaySessionStore) extends AbstractController(configuration, playSessionStore) {
//
//
//  //
//  //
//  //  private val fileSystem: FileSystem = {
//  //    val conf = new org.apache.hadoop.conf.Configuration()
//  //    FileSystem.get(conf)
//  //  }
//  //
//  //  @tailrec
//  //  private def addClassPathJars(sparkContext: SparkContext, classLoader: ClassLoader): Unit = {
//  //    classLoader match {
//  //      case urlClassLoader: URLClassLoader =>
//  //        urlClassLoader.getURLs.foreach { classPathUrl =>
//  //          if (classPathUrl.toExternalForm.endsWith(".jar") && !classPathUrl.toExternalForm.contains("test-interface")) {
//  //            sparkContext.addJar(classPathUrl.toExternalForm)
//  //          }
//  //        }
//  //      case _ =>
//  //    }
//  //    if (classLoader.getParent != null) {
//  //      addClassPathJars(sparkContext, classLoader.getParent)
//  //    }
//  //  }
//
//  UserGroupInformation.loginUserFromSubject(null)
//
//  private val proxyUser = UserGroupInformation.getCurrentUser
//
//  val storageManager = PhysicalDatasetController(configuration.underlying)
//
//  /**
//    *  <p>You can set the following configurations as Map[String, String]:</p>
//    *  <ul>
//    *  <li><em>"protocol": String (default "hdfs")</em>. It could be:
//    *  <ul>
//    *  <li>hdfs</li>
//    *  <li>opentsdb</li>
//    *  <li>kudu</li>
//    *  </ul>
//    *  </li>
//    *  <li>"<em>format</em>"<em>: String</em><em> (default "parquet")</em>. Defined only for hdfs protocol. It could be:
//    *  <ul>
//    *  <li>parquet</li>
//    *  <li>avro</li>
//    *  <li>csv</li>
//    *  </ul>
//    *  </li>
//    *  <li><em>"path": String</em>. Path of the Dataset in hdfs. Defined only for hdfs protocol.</li>
//    *  <li><em>"limit": Int </em>. Maximun number of rows to return. Defined for hdfs, opentsdb and kudu protocols.</li>
//    *  <li><em>"table": String</em>. kudu Table. Defined only for kudu protocol.</li>
//    *  <li><em>"metric": String</em>. Opentsdb Metric. Defined only for opentsdb protocol.</li>
//    *  <li><em>"tags": Map[String, String]</em>. Opentsdb Tags. Defined only for opentsdb protocol.</li>
//    *  <li><em>"interval":(Long, Long)</em>. Opentsdb Interval. Defined only for opentsdb protocol.</li>
//    *  </ul>
//    * @return DataFrame as json
//    */
//  @ApiOperation(
//    value = "dictionary of input parameters",
//    produces = "application/json",
//    httpMethod = "POST",
//    authorizations = Array(new Authorization(value = "basicAuth"))
//  )
//  def getDataset: Action[AnyContent] =
//    Action {
//      CheckedAction(exceptionManager orElse hadoopExceptionManager) {
//        HadoopDoAsAction {
//          request =>
//            val params = request.body.asJson.map(_.as[Map[String, String]]).getOrElse(Map.empty[String,String])
//
//            val user = request.session.get("username")
//            Logger.info(s"Request for user: $user")
//
//            storageManager.get(params) match {
//              case Success(df) =>
//                //val doc = s"[${df.collect().map(row => {Utility.rowToJson(df.schema)(row)}).mkString(",")}]"
//                val doc = s"[${df.toJSON.collect().mkString(",")}]"
//                Ok(doc)
//              case Failure(ex) =>
//                Logger.error(s"Error for input params $params. ERROR message: ${ex.getMessage}")
//                BadRequest(ex.getMessage).as(JSON)
//
//            }
//        }
//      }
//    }
//
//  /**
//    *  <p>You can set the following configurations as Map[String, String]:</p>
//    *  <ul>
//    *  <li><em>"protocol": String (default "hdfs")</em>. It could be:
//    *  <ul>
//    *  <li>hdfs</li>
//    *  <li>opentsdb</li>
//    *  <li>kudu</li>
//    *  </ul>
//    *  </li>
//    *  <li>"<em>format</em>"<em>: String</em><em> (default "parquet")</em>. Defined only for hdfs protocol. It could be:
//    *  <ul>
//    *  <li>parquet</li>
//    *  <li>avro</li>
//    *  <li>csv</li>
//    *  </ul>
//    *  </li>
//    *  <li><em>"path": String</em>. Path of the Dataset in hdfs. Defined only for hdfs protocol.</li>
//    *  <li><em>"limit": Int </em>. Maximun number of rows to return. Defined for hdfs, opentsdb and kudu protocols.</li>
//    *  <li><em>"table": String</em>. kudu Table. Defined only for kudu protocol.</li>
//    *  <li><em>"metric": String</em>. Opentsdb Metric. Defined only for opentsdb protocol.</li>
//    *  <li><em>"tags": Map[String, String]</em>. Opentsdb Tags. Defined only for opentsdb protocol.</li>
//    *  <li><em>"interval":(Long, Long)</em>. Opentsdb Interval. Defined only for opentsdb protocol.</li>
//    *  </ul>
//    * @return StructType which describes the Dataframe'schema. It is encoded as Json
//    */
//  @ApiOperation(
//    value = "dictionary of input parameters",
//    produces = "application/json",
//    httpMethod = "POST",
//    authorizations = Array(new Authorization(value = "basicAuth"))
//  )
//  def getDatasetSchema: Action[AnyContent] =
//    Action {
//      CheckedAction(exceptionManager) {
//        HadoopDoAsAction { request =>
//
//          val params = request.body.asJson.map(_.as[Map[String, String]]).getOrElse(Map.empty[String,String])
//          val newParams = params + ("limit" -> 1)
//          val res: Try[StructType] = storageManager.get(params).map(_.schema)
//
//          res match {
//            case Success(st) =>
//              Ok(st.json)
//            case Failure(ex) => Logger.error(s"Error for input params $params. ERROR message: ${ex.getMessage}")
//              Ok(ex.getMessage).as(JSON)
//
//          }
//        }
//      }
//    }
//
//}
//
