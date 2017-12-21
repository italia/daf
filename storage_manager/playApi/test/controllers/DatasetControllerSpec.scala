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
