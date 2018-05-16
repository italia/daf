package it.teamdigitale.filesystem

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
