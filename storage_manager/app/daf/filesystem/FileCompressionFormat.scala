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

sealed trait FileCompressionFormat extends FileFormat

case object NoCompressionFormat extends FileCompressionFormat with NoExtensions with NoCompression

case object LZOCompressionFormat extends FileCompressionFormat with SingleExtension {
  val extension = "lzo"
  val compressionRatio = 0.35
}

case object GZipCompressionFormat extends FileCompressionFormat {
  val extensions = Set("gzip", "gz")
  val compressionRatio = 0.12
}

case object SnappyCompressionFormat extends FileCompressionFormat with SingleExtension {
  val extension = "snappy"
  val compressionRatio = 0.21
}

object FileCompressionFormats {

  private val validCompressions = lzo.extensions ++ gzip.extensions ++ snappy.extensions

  def none: FileCompressionFormat   = NoCompressionFormat

  def lzo: FileCompressionFormat    = LZOCompressionFormat

  def gzip: FileCompressionFormat   = GZipCompressionFormat

  def snappy: FileCompressionFormat = SnappyCompressionFormat

  def isValid(format: String) = validCompressions contains format

  def unapply(candidate: String): Option[FileCompressionFormat] =
    if      (lzo.extensions    contains candidate) Some { lzo }
    else if (gzip.extensions   contains candidate) Some { gzip }
    else if (snappy.extensions contains candidate) Some { snappy }
    else if (candidate.isEmpty) Some { none }
    else None

  private def findCandidates(parts: List[String], candidates: List[FileCompressionFormat] = List.empty): List[FileCompressionFormat] = parts match {
    case FileCompressionFormats(part) :: others => findCandidates(others, part :: candidates)
    case _ :: others                            => findCandidates(others, candidates)
    case Nil                                    => candidates
  }

  def fromName(name: String) = findCandidates { name.split("\\.").toList }.headOption getOrElse NoCompressionFormat

}

