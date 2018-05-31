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

class FileCompressionFormatSpec extends FlatSpec with Matchers {

  "Uncompressed files" must "be correctly categorized" in {
    FileCompressionFormats.fromName { "path/to/data.json" } should be { FileCompressionFormats.none }
  }

  "lzo compressed files" must "be correctly categorized" in {
    FileCompressionFormats.fromName { "path/to/data.lzo.avro" } should be { FileCompressionFormats.lzo }
    FileCompressionFormats.fromName { "path/to/data.lzo" } should be { FileCompressionFormats.lzo }
    FileCompressionFormats.fromName { "path/to/data.parq.lzo" } should be { FileCompressionFormats.lzo }
  }

  "snappy compressed files" must "be correctly categorized" in {
    FileCompressionFormats.fromName { "path/to/data.snappy.json" } should be { FileCompressionFormats.snappy }
    FileCompressionFormats.fromName { "path/to/data.snappy" } should be { FileCompressionFormats.snappy }
    FileCompressionFormats.fromName { "path/to/data.avro.snappy" } should be { FileCompressionFormats.snappy }
  }

  "gzip data files" must "be correctly categorized" in {
    FileCompressionFormats.fromName { "path/to/data.gzip.parquet" } should be { FileCompressionFormats.gzip }
    FileCompressionFormats.fromName { "path/to/data.gzip" } should be { FileCompressionFormats.gzip }
    FileCompressionFormats.fromName { "path/to/data.json.gzip" } should be { FileCompressionFormats.gzip }
  }

}
