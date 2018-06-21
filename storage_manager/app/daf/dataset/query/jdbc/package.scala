package daf.dataset.query

import cats.data.WriterT
import cats.instances.try_.catsStdInstancesForTry
import doobie.util.fragment.Fragment

import scala.util.Try

package object jdbc {

  type QueryFragmentWriter[A] = WriterT[Try, Fragment, A]

  object QueryFragmentWriter {

    def apply[A](f: => Try[(Fragment, A)]): QueryFragmentWriter[A] = WriterT { f }

    def tell(f: => Fragment): QueryFragmentWriter[Unit] = WriterT.tell[Try, Fragment] { f }

  }

}
