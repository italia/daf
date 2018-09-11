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

package it.gov.daf.common

import cats.~>

import scala.concurrent.Future
import scala.language.higherKinds
import scala.util.{ Failure, Success, Try }

package object utils {

  implicit val tryFutureNat: (Try ~> Future) = new (Try ~> Future) {
    def apply[A](fa: Try[A]): Future[A] = fa match {
      case Success(a)     => Future.successful(a)
      case Failure(error) => Future.failed[A](error)
    }
  }

  /**
    * Adds syntax to types that can be naturally transformed
    */
  implicit class NatSyntax[F[_], A](f: F[A]) {

    /**
      * Transforms `F` into `G` using the available natural transformation.
      */
    def natural[G[_]](implicit nat: F ~> G): G[A] = nat(f)

    /**
      * Alias for `natural`.
      */
    def ~>[G[_]](implicit nat: F ~> G): G[A] = natural[G]

  }

}
