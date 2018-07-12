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

package daf.dataset.query

import cats.Monoid
import cats.data.WriterT
import cats.free.Free
import cats.instances.try_.catsStdInstancesForTry
import cats.instances.unit.catsKernelStdAlgebraForUnit
import doobie.util.fragment.Fragment

import scala.util.{ Failure, Success, Try }

package object jdbc {

  type Row    = List[AnyRef]
  type Header = Seq[String]

  type QueryFragmentWriter[A] = WriterT[Try, Fragment, A]
  type Trampoline[A] = Free[Try, A]

  private[jdbc] def recursionError[A](error: Throwable): Trampoline[A] = Free.liftF[Try, A] { Failure(error) }

  implicit class QueryFragmentWriterSyntax[A](writer: QueryFragmentWriter[A]) {

    /**
      * Writes out the `Fragment`, ignoring the `A` value.
      */
    def write: Try[Fragment] = writer.run.map { _._1 }

  }

  /**
    * Utility companion object for the [[QueryFragmentWriter]] synthetic type.
    */
  object QueryFragmentWriter {

    /**
      * Creates a [[QueryFragmentWriter]] instance directly from an attempt.
      */
    def apply[A](f: => Try[(Fragment, A)]): QueryFragmentWriter[A] = WriterT { f }

    /**
      * Creates a [[QueryFragmentWriter]] instance directly from a `Fragment`, not caring about errors.
      */
    def tell(f: => Fragment): QueryFragmentWriter[Unit] = WriterT.tell[Try, Fragment] { f }

    /**
      * Creates a [[QueryFragmentWriter]] instance directly from a `Fragment`.
      */
    def ask(f: => Try[Fragment]): QueryFragmentWriter[Unit] = WriterT[Try, Fragment, Unit] {
      f.map { (_, ()) }
    }

    /**
      * Creates successful but empty [[QueryFragmentWriter]] instance.
      */
    def empty[A](implicit M: Monoid[A]): QueryFragmentWriter[A] = WriterT.liftF[Try, Fragment, A] { Success(M.empty) }

    /**
      * Creates a [[QueryFragmentWriter]] instance that does nothing.
      */
    def unit: QueryFragmentWriter[Unit] = empty[Unit]

  }

}
