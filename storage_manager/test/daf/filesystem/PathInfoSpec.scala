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

import java.io.FileNotFoundException

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{ FileSystem, Path }
import org.scalatest.matchers.{ HavePropertyMatchResult, HavePropertyMatcher }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.reflect.ClassTag
import scala.util.{ Random, Try }

class PathInfoSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private implicit val fileSystem = FileSystem.getLocal(new Configuration)

  private val baseDir = "test-dir".asHadoop

  private val workingDir = baseDir / f"path-info-spec-${Random.nextInt(10000)}%05d"

  private val files = Set(
    workingDir / "test-file-1.avro",
    workingDir / "test-file-2.avro",
    workingDir / "test-file-3.parq",
    workingDir / "test-file-4.snappy",
    workingDir / "test-file-5.json.gzip"
  )

  private def createWorkingDir = Try { fileSystem.mkdirs(workingDir) }

  private def createSuccessFile = Try { fileSystem.create(workingDir / "_SUCCESS").close() }

  private def createFiles = Try {
    files.foreach { fileSystem.create(_).close() }
  }

  private def prepareData = for {
    _ <- createWorkingDir
    _ <- createSuccessFile
    _ <- createFiles
  } yield ()

  private def purgeData = Try { fileSystem.delete(workingDir, true) }

  override def beforeAll() = prepareData.get

  override def afterAll() = purgeData.get

  "Path info" when {

    "given a non-existent file" must {

      "throw an exception" in {
        a[FileNotFoundException] must be thrownBy PathInfo.fromHadoop { workingDir / "non-existent-test-file.json.gzip" }
      }
    }

    "given an existing file or directory" must {

      "retrieve file information correctly" in {
        PathInfo.fromHadoop { workingDir / "test-file-5.json.gzip" } should be {
          FileInfo(workingDir.resolve / "test-file-5.json.gzip", 0, FileDataFormats.json, FileCompressionFormats.gzip)
        }
      }

      "retrieve directory information correctly" in {
        PathInfo.fromHadoop { workingDir } should have (
          PathInfoMatchers.path { workingDir.resolve },
          PathInfoMatchers.isDir { true },
          PathInfoMatchers.file { FileInfo(workingDir.resolve / "test-file-5.json.gzip", 0, FileDataFormats.json, FileCompressionFormats.gzip) },
          PathInfoMatchers.file { FileInfo(workingDir.resolve / "test-file-4.snappy", 0, FileDataFormats.raw, FileCompressionFormats.snappy) },
          PathInfoMatchers.file { FileInfo(workingDir.resolve / "test-file-3.parq", 0, FileDataFormats.parquet, FileCompressionFormats.none) },
          PathInfoMatchers.file { FileInfo(workingDir.resolve / "test-file-2.avro", 0, FileDataFormats.avro, FileCompressionFormats.none) },
          PathInfoMatchers.file { FileInfo(workingDir.resolve / "test-file-1.avro", 0, FileDataFormats.avro, FileCompressionFormats.none) }
        )
      }

      "exclude the _SUCCESS file" in {
        PathInfo.fromHadoop { workingDir } should not have {
          PathInfoMatchers.file { FileInfo(workingDir.resolve / "_SUCCESS", 0, FileDataFormats.raw, FileCompressionFormats.none) }
        }
      }
    }
  }
}

private object PathInfoMatchers {

  private def refine[A <: PathInfo](pathInfo: PathInfo)(implicit A: ClassTag[A]) = pathInfo match {
    case a: A => a
    case _    => throw new IllegalArgumentException(s"Invalid PathInfo type: expected [${A.runtimeClass.getName}] but encountered [${pathInfo.getClass.getName}]")
  }

  def isDir(expectedValue: Boolean) = new HavePropertyMatcher[PathInfo, Boolean] {
    def apply(pathInfo: PathInfo) = HavePropertyMatchResult(
      pathInfo.isDir == expectedValue,
      "isDir",
      expectedValue,
      pathInfo.isDir
    )
  }

  def path(expectedValue: Path) = new HavePropertyMatcher[PathInfo, Path] {
    def apply(pathInfo: PathInfo) = HavePropertyMatchResult(
      pathInfo.path == expectedValue,
      "path",
      expectedValue,
      pathInfo.path
    )
  }

  private def fileMatcher(expectedValue: FileInfo) =  new HavePropertyMatcher[DirectoryInfo, Seq[FileInfo]] {
    def apply(dirInfo: DirectoryInfo) = HavePropertyMatchResult(
      dirInfo.files contains expectedValue,
      "files",
      Seq(expectedValue),
      dirInfo.files
    )
  }

  def file(fileInfo: FileInfo) = new HavePropertyMatcher[PathInfo, Seq[FileInfo]] {
    def apply(pathInfo: PathInfo) = fileMatcher(fileInfo) { refine[DirectoryInfo](pathInfo) }
  }

}
