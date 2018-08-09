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

package daf.filesystem

import org.scalatest.{ FlatSpec, Matchers }

class FileDataFormatSpec extends FlatSpec with Matchers {

  "Raw data files" must "be correctly categorized" in {
    FileDataFormats.fromName { "path/to/data" } should be { FileDataFormats.raw }
  }

  "Json data files" must "be correctly categorized" in {
    FileDataFormats.fromName { "path/to/data.json" } should be { FileDataFormats.json }
  }

  "Parquet data files" must "be correctly categorized" in {
    FileDataFormats.fromName { "path/to/data.parq"    } should be { FileDataFormats.parquet }
    FileDataFormats.fromName { "path/to/data.parquet" } should be { FileDataFormats.parquet }
  }

  "Avro data files" must "be correctly categorized" in {
    FileDataFormats.fromName { "path/to/data.avro" } should be { FileDataFormats.avro }
  }

  "Compressed data files" must "be correctly categorized" in {
    FileDataFormats.fromName { "path/to/data.snappy.avro" } should be { FileDataFormats.avro }
    FileDataFormats.fromName { "path/to/data.parquet.gzip" } should be { FileDataFormats.parquet }
  }


}
