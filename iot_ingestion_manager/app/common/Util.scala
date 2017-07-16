package common

import com.typesafe.config.ConfigException.Missing
import org.slf4j.Logger
import play.api.Configuration

import scala.util.{Failure, Success, Try}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Throw"
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
    def log(logger: Logger, errorMsg: String): Unit = t match {
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
