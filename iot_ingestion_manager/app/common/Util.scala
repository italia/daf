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

package common

import com.typesafe.config.ConfigException.Missing
import play.Logger.ALogger
import play.api.Configuration

import scala.util.{Failure, Success, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.ImplicitParameter"
  )
)
object Util {

  implicit class OptionTry[A](val oa: Option[A]) extends AnyVal {

    def asTry[E <: Exception](e: E): Try[A] = oa match {
      case Some(a) => Success(a)
      case None => Failure(e)
    }
  }

  implicit class EnrichedTry[A](val t: Try[A]) extends AnyVal {
    def log(errorMsg: String)(implicit logger: ALogger): Unit = t match {
      case Success(v) => logger.info(s"$v")
      case Failure(e) => logger.error(s"$errorMsg: ${e.getMessage}")
    }
  }

  implicit class EnrichedConfiguration(val configuration: Configuration) extends AnyVal {
    def getIntOrException(path: String): Int = configuration.getInt(path).getOrElse(throw new Missing(path))

    def getLongOrException(path: String): Long = configuration.getLong(path).getOrElse(throw new Missing(path))

    def getStringOrException(path: String): String = configuration.getString(path).getOrElse(throw new Missing(path))
  }

  def InitializeFailure[A]: Try[A] = Failure[A](new Exception("Initialization Error"))

}
