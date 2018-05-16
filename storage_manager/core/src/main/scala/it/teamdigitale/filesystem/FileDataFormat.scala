package it.teamdigitale.filesystem

sealed trait FileDataFormat extends FileFormat

case object RawFileFormat extends FileDataFormat with NoExtensions with NoCompression

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

  def avro: FileDataFormat    = AvroFileFormat

  def isValid(format: String) = validExtensions contains format

  def unapply(candidate: String): Option[FileDataFormat] =
    if      (parquet.extensions contains candidate) Some { parquet }
    else if (json.extensions    contains candidate) Some { json }
    else if (avro.extensions    contains candidate) Some { avro }
    else if (candidate.isEmpty) Some { raw }
    else None

  private def findCandidates(parts: List[String], candidates: List[FileDataFormat] = List.empty): List[FileDataFormat] = parts match {
    case FileDataFormats(part) :: others => findCandidates(others, part :: candidates)
    case _ :: others                     => findCandidates(others, candidates)
    case Nil                             => candidates
  }

  def fromName(name: String) = findCandidates { name.split("\\.").toList }.headOption getOrElse RawFileFormat

}
