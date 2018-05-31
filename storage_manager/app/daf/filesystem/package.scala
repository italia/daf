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

package daf

import java.io.EOFException
import java.nio.charset.Charset
import java.util.Scanner

import org.apache.hadoop.fs.{ FSDataInputStream, FileSystem, Path }

package object filesystem {

  implicit class StringPathSyntax(path: String) {

    def asHadoop = new Path(path)

    def /(other: String) = s"$path/$other"

  }

  implicit class HadoopPathSyntax(path: Path) {

    def /(other: String) = new Path(path, other)

    def /(other: Path) = new Path(path, other)

    def asUriString = path.toUri.getPath

    def resolve(implicit fileSystem: FileSystem) = fileSystem.getFileStatus(path).getPath

  }

  implicit class FSDataInputStreamSyntax(inputStream: FSDataInputStream) {

    private val encoding = Charset.forName("UTF-8")

    def scanner = new Scanner(inputStream, encoding.name)

    def scan[A](f: Scanner => A) = f { scanner }

    /**
      * Attempts to drop the next line and returns the number of bytes read.
      */
    def skim = Stream.continually { inputStream.read() }.takeWhile {
      case '\n' | '\r' | -1 => false
      case _                => true
    }.size

    /**
      * Advances the stream to the next line. This has the same effect on the stream as calling `skim`. If nothing was
      * dropped, the stream of empty, and we throw `EOFException`.
      *
      * @throws EOFException if the stream was empty
      */
    def tail: FSDataInputStream = if (skim > 0) inputStream else throw new EOFException("Attempted to tail an empty stream")

  }

}
