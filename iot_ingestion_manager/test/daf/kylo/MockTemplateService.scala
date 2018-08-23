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

package daf.kylo

import daf.util.MockService
import org.apache.nifi.web.api.dto.TemplateDTO

import scala.collection.convert.decorateAsJava._

sealed class MockTemplateService extends MockService {

  private def configTemplate(name: String, templateDTO: TemplateDTO = new TemplateDTO) = {
    templateDTO.setName    { name           }
    templateDTO.setId      { s"$name-id"    }
    templateDTO.setGroupId { s"$name-group" }
    templateDTO
  }

  private val templates = Set(
    configTemplate("test-template")
  )

  def allTemplates = ok { templates.asJava }

}

object MockTemplateService {

  def init() = new MockTemplateService

}
