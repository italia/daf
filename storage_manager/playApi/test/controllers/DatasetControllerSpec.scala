package controllers

import com.typesafe.config.ConfigFactory
import it.gov.daf.server.dataset.DatasetService
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.Configuration
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

class DatasetControllerSpec extends PlaySpec with MockitoSugar with Results {

  val mockedDatasetService = mock[DatasetService]

  val conf = ConfigFactory.load("test.conf")

//  val controller = new DatasetController(new Configuration(conf)){
//    override val datasetService: DatasetService = mockedDatasetService
//  }

//  "The dataset manager" should "get schema" in

}
