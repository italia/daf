package daf.dataset.query

import cats.data.WriterT
import cats.free.Free
import cats.instances.try_.catsStdInstancesForTry
import doobie.util.fragment.Fragment

import scala.util.{ Failure, Try }

package object jdbc {

  type QueryFragmentWriter[A] = WriterT[Try, Fragment, A]
  type Trampoline[A] = Free[Try, A]

  private[jdbc] def recursionError[A](error: Throwable): Trampoline[A] = Free.liftF[Try, A] { Failure(error) }

  object QueryFragmentWriter {

    def apply[A](f: => Try[(Fragment, A)]): QueryFragmentWriter[A] = WriterT { f }

    def tell(f: => Fragment): QueryFragmentWriter[Unit] = WriterT.tell[Try, Fragment] { f }

    def ask(f: => Try[Fragment]): QueryFragmentWriter[Unit] = WriterT[Try, Fragment, Unit] {
      f.map { _ -> (()) }
    }

  }

}
