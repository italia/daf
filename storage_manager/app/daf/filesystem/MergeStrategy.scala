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
import java.util.Collections

import org.apache.commons.io.input.NullInputStream
import org.apache.hadoop.fs.FSDataInputStream

import scala.annotation.tailrec
import scala.collection.convert.decorateAsJava._

trait MergeStrategy[+A <: FileDataFormat] {

  /**
    * Merges an ordered sequence of streams into one. The implementor need not guarantee that all streams will be
    * merged as-is; on the contrary, callers should expect some manipulation before the streams are merged into one.
    * @param streams '''ordered''' sequence of streams to be merged
    * @return a single `InputStream` that is the result of merging the input
    */
  def merge(streams: Seq[FSDataInputStream]): InputStream

}

object MergeStrategy {

  @tailrec
  private def _separate(streams: List[InputStream], acc: Seq[InputStream] = Vector.empty): Seq[InputStream] = streams match {
    case Nil                          => acc
    case head :: Nil  if acc.nonEmpty => acc :+ nextStream :+ head
    case head :: Nil                  => acc :+ head
    case head :: tail if acc.nonEmpty => _separate(tail, acc :+ nextStream :+ head)
    case head :: tail                 => _separate(tail, acc :+ head)
  }

  /**
    * Filters out empty streams and closes them immediately. Note that this method makes use of the `available` method,
    * which may be noted to be overridden to not function as intended by certain subclasses of `InputStream`. This
    * merge function should therefore be used with caution, knowing which `InputStream`s are being passed as arguments.
    * Also note that any empty `InputStream`s that are found to be empty will be closed.
    * @param streams the streams to filter
    */
  def nonEmpty(streams: Seq[InputStream]) = streams.partition { _.available() > 0 } match {
    case (nonEmpty, empty) => empty.foreach { _.close() }; nonEmpty
  }

  /**
    * Adds a separator stream between every two streams, containing just a new-line character. This is useful when a
    * number of files is being concatenated, which do not end in an empty new-line character.
    * @param streams the streams to be separated
    */
  def separated(streams: Seq[InputStream]) = _separate(streams.toList)

  /**
    * Coalesces all the supplied streams into one `SequenceInputStream`.
    * @param streams the streams to be concatenated
    */
  def coalesced(streams: Seq[InputStream]) = new SequenceInputStream(
    Collections.enumeration { streams.asJava }
  )

  /**
    * The default merging base, this function will concatenate all the given non-empty streams, closing those that are
    * found to be empty.
    */
  def default(streams: Seq[InputStream]) = coalesced { nonEmpty(streams) }

}

object CsvMergeStrategy extends MergeStrategy[RawFileFormat.type] {

  private def clean(streams: Seq[FSDataInputStream]) = streams match {
    case Seq(head, tail @ _*) => head +: tail.flatMap { _.tailOption }
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
    case cleaned if cleaned.nonEmpty => MergeStrategy.default(cleaned)
    case _                           => new NullInputStream(0)
  }
}

class DefaultMergeStrategy[A <: FileDataFormat] extends MergeStrategy[A] {

  def merge(streams: Seq[FSDataInputStream]) = if (streams.nonEmpty) MergeStrategy.default(streams) else new NullInputStream(0)

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