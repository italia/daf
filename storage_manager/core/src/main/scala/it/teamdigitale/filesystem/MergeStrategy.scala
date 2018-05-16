package it.teamdigitale.filesystem

import java.io._

import org.apache.commons.io.input.NullInputStream
import org.apache.hadoop.fs.FSDataInputStream

trait MergeStrategy[+A <: FileDataFormat] {

  def merge(streams: Seq[FSDataInputStream]): InputStream

}

object CsvMergeStrategy extends MergeStrategy[RawFileFormat.type] {

  private def clean(streams: Seq[FSDataInputStream]) = streams match {
    case Seq(head, tail @ _*) => head +: tail.map { _.tail }
    case _                    => streams
  }

  /**
    * @inheritdoc
    *
    * Merges the input streams into one, retaining the header only from the first input stream in the list.
    * @param streams list of streams to merge, where the header of the first one will be retained, and that of the
    *                remainder will be dropped.
    * @return one input stream with whose content is the concatenation of the all the input streams
    */
  def merge(streams: Seq[FSDataInputStream]) = clean(streams) match {
    case cleaned if cleaned.nonEmpty => cleaned.reduce[InputStream] { new SequenceInputStream(_, _) }
    case _                           => new NullInputStream(0)
  }
}

class DefaultMergeStrategy[A <: FileDataFormat] extends MergeStrategy[A] {

  def merge(streams: Seq[FSDataInputStream]) = if (streams.nonEmpty) streams.reduce[InputStream] { new SequenceInputStream(_, _) } else new NullInputStream(0)

}

object MergeStrategies {

  implicit val json = new DefaultMergeStrategy[JsonFileFormat.type]

  implicit val csv = CsvMergeStrategy

  implicit val default = new DefaultMergeStrategy[FileDataFormat]

  def find(fileFormat: FileDataFormat): MergeStrategy[FileDataFormat] = fileFormat match {
    case JsonFileFormat => json
    case RawFileFormat  => csv
    case other          => throw new IllegalArgumentException(s"Unable to find a merge strategy for file format of type [${other.getClass.getSimpleName}]")
  }

  def find(fileInfo: FileInfo): MergeStrategy[FileDataFormat] = fileInfo.compression match {
    case NoCompressionFormat => find(fileInfo.format)
    case _                   => throw new IllegalArgumentException(s"Cannot determine a merge strategy for compressed file")
  }

}