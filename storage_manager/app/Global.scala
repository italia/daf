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

import java.io.File
import java.net.{URL, URLClassLoader}

import com.google.inject.{AbstractModule, Singleton}

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
object HadoopConfDir {

  var hadoopConfDir: Option[String] = None
}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Overloading",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.StringPlusAny",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.Var"
  )
)
@Singleton
class Module extends AbstractModule {

  println("STEP2")

  def addPath(dir: String): Unit = {
    val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
    method.setAccessible(true)
    method.invoke(Thread.currentThread().getContextClassLoader, new File(dir).toURI.toURL)
    ()
  }

  def configure(): Unit = {
    println("STEP3")
    HadoopConfDir.hadoopConfDir.foreach(addPath(_))
  }
}