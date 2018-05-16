package it.teamdigitale.filesystem

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
