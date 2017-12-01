package it.gov.daf.ingestion.nifi

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Try

class NifiHelperSpec extends FlatSpec with Matchers with PropertyChecks {



  "A NifiHelper " should "create a valid connection" in {

      val result = Try(NifiHelper.defineConnection(
        clientId = "1",
        name = "1",
        sourceId = "1",
        sourceGroupId = "a",
        sourceType = "PROCESSOR",
        destId = "2",
        destGroupId = "a",
        destType = "PROCESSOR"
      ))

      result.isSuccess shouldBe true
    }
}
