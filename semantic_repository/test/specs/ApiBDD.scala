package specs

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import play.api.test.WithBrowser

@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification {

  val host = "localhost"

  "lod-manager" should {

    "expose swagger specification" in new WithBrowser {
      browser.goTo(s"http://${host}:${port}/spec/lod_manager.yaml")
      browser.pageSource must haveSize(greaterThan(0))
      browser.pageSource must contain("LOD Manager")
    }

    "list all existing contexts" in new WithBrowser {
      browser.goTo(s"http://${host}:${port}/kb/v1/contexts")
      browser.pageSource must haveSize(greaterThan(0))

      val ctxs = JSONHelper.parseString(browser.pageSource).toList
      ctxs.find { el => el.get("context").equals("http://xmlns.com/foaf/0.1/") }
      ctxs.find { el => el.get("triples").equals(631) }
    }

  }
}

object JSONHelper {

  val json_mapper = new ObjectMapper
  val json_reader = json_mapper.reader()

  val cc = ConfigFactory.empty()

  def parseString(json: String) = json_reader.readTree(json)

}