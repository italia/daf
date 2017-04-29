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
import javax.inject.Inject

import com.google.inject.{AbstractModule, ImplementedBy, Singleton}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future
import scala.util.Try

@Singleton
class Global @Inject()(lifecycle: ApplicationLifecycle) {
  lifecycle.addStopHook { () => Future.successful({}) }
}

@ImplementedBy(classOf[HadoopModule])
trait WithFileSystem {
  def fs: Try[FileSystem]
}

@SuppressWarnings(Array("org.wartremover.warts.Overloading"))
@Singleton
class HadoopModule extends AbstractModule with WithFileSystem {

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def addPath(dir: String): Unit = {
    val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
    method.setAccessible(true)
    method.invoke(Thread.currentThread().getContextClassLoader, new File(dir).toURI.toURL)
    ()
  }

  def configure() = {

  }

  val fs: Try[FileSystem] = Try {
    FileSystem.get(new Configuration())
  }
}