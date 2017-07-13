package common

import scala.util.{Failure, Success, Try}

object Util {

  implicit class OptionTry[A](val oa: Option[A]) extends AnyVal {

    def asTry[E <: Exception](e: E): Try[A] = oa match {
      case Some(a) => Success(a)
      case None => Failure(e)
    }
  }

}
