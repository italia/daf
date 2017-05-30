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

import java.io.File
import java.util

import de.zalando.play.controllers.PlayBodyParsing._
import org.yaml.snakeyaml.Yaml
import play.api.mvc._

import scala.io.Source

class Swagger extends Controller {

  def notSpec = Seq("application.conf", "logback.xml", "routes")

  def listSpecs() = Action {
    val path = "conf"
    val file = new File(path)
    if (file != null && file.list() != null) {
      val files = file.listFiles().filterNot(_.isDirectory).map(_.getName).toSeq
      implicit val arrayMarshaller = anyToWritable[Seq[String]]("application/json")
      val names = files.filterNot(notSpec.contains).filterNot(_.startsWith("."))
      Ok(names)
    } else {
      NotFound("Path could not be found: " + file.getAbsolutePath)
    }
  }

  def swaggerSpec(name: String) = Action {
    implicit val mapMarshaller = anyToWritable[java.util.Map[_, _]]("application/json")
    getSpec(name).map(s => Ok(s)).getOrElse(NotFound(name))
  }

  private def getSpec(yamlPath: String) = {
    val yamlFile = Option(getClass.getClassLoader.getResource(yamlPath))
    val yamlStr = yamlFile map { yaml => Source.fromURL(yaml).getLines().mkString("\n") }
    val javaMap = yamlStr map { new Yaml().load(_).asInstanceOf[util.Map[Any, Any]] }
    javaMap
  }
}
