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

package daf.dataset

import akka.stream.scaladsl.Source
import cats.syntax.show.toShow
import controllers.DatasetController
import daf.dataset.query.jdbc.JdbcResult
import daf.dataset.query.{ Query => NewQuery }
import daf.filesystem._
import org.apache.hadoop.fs.Path
import play.api.libs.json.JsValue

import scala.util.{ Failure, Success, Try }

trait QueryExecution { this: DatasetController =>

  private def extractTableName(path: Path): Try[String] = Try { s"${path.getParent.getName}.${path.getName}" }

  private def extractTableName(params: DatasetParams): Try[String] = params match {
    case kudu: KuduDatasetParams => Success { kudu.table }
    case file: FileDatasetParams => extractTableName(file.path.asHadoop.resolve)
  }

  private def transform(jdbcResult: JdbcResult, targetFormat: FileDataFormat) = targetFormat match {
    case CsvFileFormat  => Try {
      Source[String](jdbcResult.toCsv).map { csv => s"$csv${System.lineSeparator}" }
    }
    case JsonFileFormat => Try {
      Source[JsValue](jdbcResult.toJson).map { json => s"$json${System.lineSeparator}"}
    }
    case _              => Failure { new IllegalArgumentException(s"Invalid target format [$targetFormat]; must be [csv | json]") }
  }

  private def respond(params: DatasetParams, data: Source[String, _], targetFormat: FileDataFormat) = Try {
    Ok.chunked(data).withHeaders(
      CONTENT_DISPOSITION -> s"""attachment; filename="${params.name}.${targetFormat.show}""""
    )
  }

  protected def exec(params: DatasetParams, query: NewQuery, targetFormat: FileDataFormat, userId: String) = for {
    tableName  <- extractTableName(params)
    jdbcResult <- queryService.exec(query, tableName, userId)
    data       <- transform(jdbcResult, targetFormat)
    response   <- respond(params, data, targetFormat)
  } yield response

}