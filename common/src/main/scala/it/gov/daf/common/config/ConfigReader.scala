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

package it.gov.daf.common.config

import play.api.Configuration

import scala.util.{ Try, Success, Failure }

/**
  * Representation of a lazy read on configuration, with safe attempt. Serves mainly as a wrapper for composition
  * purposes, with limited capabilities.
  *
  * @note This can be easily superseded by better and more general category transformations such as `Kleisli` associated
  *       with [[scala.util.Try]].
  *
  * @param read the binding attempt from a `Configuration` to `A`
  * @tparam A
  */
sealed abstract class ConfigReader[A](val read: Configuration => Try[A]) {

  def map[B](f: A => B): ConfigReader[B]

  def flatMap[B](f: A => ConfigReader[B]): ConfigReader[B]

  /**
    * Will try to apply the mapping `f` to the result of this mapping as a composition, equivalent to calling
    * [[scala.util.Try#flatMap]] on the result of this [[read]].
    *
    * @param f the function to compose
    * @tparam B the return type of the composition result
    * @return a [[ConfigReader]] instance whose read attempt will result in `B`
    */
  def mapAttempt[B](f: A => Try[B]): ConfigReader[B]

  /**
    * Alias for [[mapAttempt]] where the return type of this [[ConfigReader]] is known to be `Configuration`.
    * This allows for smooth compositions when drilling through parts of a complex configuration.
    *
    * {{{
    *   obj1 {
    *     arg1 = 1
    *     obj2 {
    *       arg2 = 2
    *     }
    *   }
    * }}}
    *
    * One way to get to `arg2` is by
    *
    * {{{
    *   readObj1 ~> readObj2 ~> readArg2
    * }}}
    *
    * @param otherReader the reader to compose after this one
    * @param ev implicit evidence that shows that this reader reads `play.api.Configuration`
    * @tparam B return type of `otherReader`
    * @return a reader that will first apply this reader's [[read]] and then `otherReader`'s
    */
  final def ~>[B](otherReader: ConfigReader[B])(implicit ev: A =:= Configuration): ConfigReader[B] = mapAttempt { a => otherReader.read(ev(a)) }

}

sealed class MandatoryConfigReader[A](f: Configuration => Try[A]) extends ConfigReader(f) {

  def map[B](ff: A => B) = new MandatoryConfigReader[B](
    f andThen { _.map(ff) }
  )

  def flatMap[B](ff: A => ConfigReader[B]) = new MandatoryConfigReader[B]( configuration =>
    f andThen {
      _.flatMap { ff(_).read(configuration) }
    } apply configuration
  )

  def mapAttempt[B](ff: A => Try[B]) = new MandatoryConfigReader[B](
    f andThen {
      _.flatMap { ff }
    }
  )

}

sealed case class OptionalConfigReader[A](private val key: String)
                                         (private val f: Configuration => Option[A])
  extends MandatoryConfigReader[Option[A]](config => Try(f(config))) {

  /**
    * Forces this optional value.
    * @return a [[ConfigReader]] that reads a mandatory value
    */
  def ! : ConfigReader[A] = new MandatoryConfigReader[A](
    this.read(_).flatMap {
      case Some(value) => Success(value)
      case None        => Failure[A] { ConfigMissingException(key) }
    }
  )

  /**
    * Adds a default value to this reader.
    * @param a the default value
    * @return a [[ConfigReader]] that reads an optional value and returns `a` if the value is missing
    */
  def default(a: => A): ConfigReader[A] = new MandatoryConfigReader(
    this.read(_).map { _ getOrElse a }
  )

}

