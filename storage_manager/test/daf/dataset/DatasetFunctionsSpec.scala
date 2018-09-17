package daf.dataset

import java.io.ByteArrayInputStream

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import controllers.modules.TestAbstractModule
import daf.filesystem.MergeStrategy
import daf.instances.{ AkkaInstance, ConfigurationInstance }
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class DatasetFunctionsSpec extends TestAbstractModule
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll
  with ConfigurationInstance
  with AkkaInstance {

  implicit lazy val executionContext = actorSystem.dispatchers.lookup("akka.actor.test-dispatcher")

  protected implicit lazy val materializer = ActorMaterializer.create { actorSystem }

  override def beforeAll() = {
    startAkka()
  }

  def data = (1 to 5) .map { i =>
    Random.alphanumeric.grouped(20).take(5).map { s => s"$i - ${s.mkString}" }.toStream :+ defaultSeparator
  }

  def stream = MergeStrategy.coalesced {
    data.map { iter =>
      new ByteArrayInputStream(
        iter.mkString(defaultSeparator).getBytes("UTF-8")
      )
    }
  }

  def source = StreamConverters.fromInputStream(() => stream, 5)

  "Source manipulation" must {

    "convert to a string source" in {
      Await.result(
        wrapDefault { asStringSource(source) }.runFold("") { _ + _ },
        5.seconds
      ).split(defaultSeparator).length must be { 25 }
    }

    "convert to a json source" in {
      Await.result(
        wrapJson { asStringSource(source) }.runFold("") { _ + _ },
        5.seconds
      ).split(jsonSeparator).length must be { 25 }
    }

  }

}
