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

import java.io._

import org.apache.commons.io.input.NullInputStream
import org.apache.hadoop.fs.FSDataInputStream

trait MergeStrategy[+A <: FileDataFormat] {

  /**
    * Merges an ordered sequence of streams into one. The implementor need not guarantee that all streams will be
    * merged as-is; on the contrary, callers should expect some manipulation before the streams are merged into one.
    * @param streams '''ordered''' sequence of streams to be merged
    * @return a single `InputStream` that is the result of merging the input
    */
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
    case CsvFileFormat  => csv
    case other          => throw new IllegalArgumentException(s"Unable to find a merge strategy for file format of type [${other.getClass.getSimpleName}]")
  }

  def find(fileInfo: FileInfo): MergeStrategy[FileDataFormat] = fileInfo.compression match {
    case NoCompressionFormat => find(fileInfo.format)
    case _                   => throw new IllegalArgumentException(s"Cannot determine a merge strategy for compressed file")
  }

}