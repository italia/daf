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

import java.io.{ Closeable, InputStream }
import java.util.Scanner

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{ FSDataInputStream, FSDataOutputStream, FileSystem, Path }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.collection.convert.decorateAsScala._
import scala.util.{ Random, Try }

class MergeStrategySpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private implicit val fileSystem = FileSystem.getLocal(new Configuration)

  private val numFiles = 10

  private val baseDir = "test-dir".asHadoop

  private val workingDir = baseDir / f"merge-strategy-spec-${Random.nextInt(10000)}%05d"

  private def safely[A <: Closeable, U](f: A => U) = { stream: A =>
    val attempt = Try { f(stream) }
    stream.close()
    attempt
  }

  private def readFile(path: Path) = safely[FSDataInputStream, Seq[String]] { _.scanner.asScala.toSeq } apply fileSystem.open(path)

  private def readFiles = Try {
    fileSystem.listStatus(workingDir).toSeq.flatMap { status => readFile(status.getPath).get }
  }

  private def openFiles = Try {
    fileSystem.listStatus(workingDir).toSeq.map { status => fileSystem.open(status.getPath) }
  }

  private def createFile(fileName: String) = safely[FSDataOutputStream, Unit] { stream =>
    Random.alphanumeric.grouped(200).take(10).map { randomSplits(_) }.foreach { row =>
      stream.writeUTF { row.mkString("", ",", "\n") }
    }
  } apply fileSystem.create { workingDir / fileName }

  private def randomSplits(chars: Stream[Char], strings: Seq[String] = Seq.empty): Seq[String] = chars.splitAt { Random.nextInt(10) + 5 } match {
    case (head, tail) if tail.isEmpty => head.drop(1).mkString +: strings
    case (head, tail)                 => randomSplits(tail, head.mkString +: strings)
  }

  private def createWorkingDir = Try { fileSystem.mkdirs(workingDir) }

  private def createFiles = Try {
    0 until numFiles foreach { index => createFile(s"test-file-$index").get } // this is relatively nasty, and should be handled in a `traverse`
  }

  private def prepareData = for {
    _ <- createWorkingDir
    _ <- createFiles
  } yield ()

  private def purgeData = Try { fileSystem.delete(workingDir, true) }

  override def beforeAll() = prepareData.get

//  override def afterAll() = purgeData.get

  "MergeStrategies info" when {

    "given compressed format files" must {

      "throw an exception" in {
        an[IllegalArgumentException] must be thrownBy MergeStrategies.find { FileInfo(workingDir / "test-file-0", 0, FileDataFormats.raw, FileCompressionFormats.gzip) }
      }
    }

    "given data as csv" must {

      "drop one line and merge the rest" in {
        safely[InputStream, Seq[String]] { new Scanner(_).asScala.toList }.andThen { attempt =>
          for {
            merged   <- attempt
            expected <- readFiles
          } merged.size should be { expected.size - numFiles + 1 }
        } apply MergeStrategies.csv.merge { openFiles.get }
      }
    }

    "given data as json" must {

      "just merge the files into one" in {
        safely[InputStream, Seq[String]] { new Scanner(_).asScala.toList }.andThen { attempt =>
          for {
            merged   <- attempt
            expected <- readFiles
          } merged.size should be { expected.size }
        } apply MergeStrategies.json.merge { openFiles.get }
      }

    }
  }
}
