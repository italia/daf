package it.teamdigitale

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
