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

sealed trait FileDataFormat extends FileFormat

case object RawFileFormat extends FileDataFormat with NoExtensions with NoCompression

case object CsvFileFormat extends FileDataFormat with SingleExtension with NoCompression {
  val extension = "csv"
}

case object ParquetFileFormat extends FileDataFormat {
  val extensions = Set("parq", "parquet")
  val compressionRatio = 0.59
}

case object JsonFileFormat extends FileDataFormat with SingleExtension with NoCompression {
  val extension = "json"
}

case object AvroFileFormat extends FileDataFormat with SingleExtension with NoCompression {
  val extension = "avro"
}

object FileDataFormats {

  private val validExtensions = parquet.extensions ++ json.extensions ++ avro.extensions

  def raw: FileDataFormat     = RawFileFormat

  def parquet: FileDataFormat = ParquetFileFormat

  def json: FileDataFormat    = JsonFileFormat

  def csv: FileDataFormat     = CsvFileFormat

  def avro: FileDataFormat    = AvroFileFormat

  def isValid(format: String) = validExtensions contains format

  def unapply(candidate: String): Option[FileDataFormat] =
    if      (parquet.extensions contains candidate) Some { parquet }
    else if (json.extensions    contains candidate) Some { json }
    else if (avro.extensions    contains candidate) Some { avro }
    else if (csv.extensions     contains candidate) Some { csv }
    else if (candidate.isEmpty) Some { raw }
    else None

  private def findCandidates(parts: List[String], candidates: List[FileDataFormat] = List.empty): List[FileDataFormat] = parts match {
    case FileDataFormats(part) :: others => findCandidates(others, part :: candidates)
    case _ :: others                     => findCandidates(others, candidates)
    case Nil                             => candidates
  }

  def fromName(name: String) = findCandidates { name.split("\\.").toList }.headOption getOrElse RawFileFormat

}
