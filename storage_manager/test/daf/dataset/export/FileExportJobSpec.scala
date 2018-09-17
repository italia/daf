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

package daf.dataset.export

import daf.filesystem._
import daf.instances.SparkSessionInstance
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.livy.JobContext
import org.apache.spark.sql.SparkSession
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.util.{ Random, Try }

class FileExportJobSpec extends WordSpec with Matchers with BeforeAndAfterAll with SparkSessionInstance {

  private implicit val fileSystem = FileSystem.getLocal(new Configuration)

  private val baseDir = "test-dir".asHadoop

  private val workingDir = baseDir / f"file-export-job-spec-${Random.nextInt(10000)}%05d"

  private val inputDir  = workingDir/ "input"

  private val outputDir = workingDir / "output"

  private def createDataFrame = sparkSession.createDataFrame {
    Stream.continually { User.random }.take { Random.nextInt(100) + 50 }
  }

  private def createDataFiles = for {
    csvPath  <- Try { inputDir / "csv" }
    jsonPath <- Try { inputDir / "json" }
    parqPath <- Try { inputDir / "parq" }
    _ <- Try { createDataFrame.write.option("header", true).option("delimiter", ",").csv(csvPath.asUriString) }
    _ <- Try { createDataFrame.write.json(jsonPath.asUriString) }
    _ <- Try { createDataFrame.write.parquet(parqPath.asUriString) }
  } yield ()

  private def purgeData = Try { fileSystem.delete(workingDir, true) }

  override def beforeAll() = {
    createDataFiles.get
  }

  override def afterAll() = {
    sparkSession.stop()
    purgeData.get
  }

  "A file export job" when {

    "reading CSV data" must {

      "convert successfully to json" in {
        FileExportJob.create(
          { inputDir / "csv" }.asUriString,
          { outputDir / "csv-json" }.asUriString,
          RawFileFormat,
          JsonFileFormat,
          limit = Some(2)
        ).call { new TestExportJobContext(sparkSession) }.asHadoop should be { outputDir / "csv-json" }
      }

      "throw error converting to parquet" in {
        a[RuntimeException] should be thrownBy {
          FileExportJob.create(
            { inputDir / "csv" }.asUriString,
            { outputDir / "csv-json" }.asUriString,
            RawFileFormat,
            ParquetFileFormat,
            limit = None
          ).call { new TestExportJobContext(sparkSession) }
        }
      }

    }

    "reading JSON data" must {

      "convert successfully to csv" in {
        FileExportJob.create(
          { inputDir / "json" }.asUriString,
          { outputDir / "json-csv" }.asUriString,
          JsonFileFormat,
          CsvFileFormat,
          limit = None
        ).call { new TestExportJobContext(sparkSession) }.asHadoop should be { outputDir / "json-csv" }
      }

      "throw error converting to parquet" in {
        a[RuntimeException] should be thrownBy {
          FileExportJob.create(
            { inputDir / "json" }.asUriString,
            { outputDir / "json-csv" }.asUriString,
            JsonFileFormat,
            ParquetFileFormat,
            limit = None
          ).call { new TestExportJobContext(sparkSession) }
        }
      }
    }

    "reading Parquet data" must {

      "convert successfully to csv" in {
        FileExportJob.create(
          { inputDir / "parq" }.asUriString,
          { outputDir / "parq-csv" }.asUriString,
          ParquetFileFormat,
          CsvFileFormat,
          limit = None
        ).call { new TestExportJobContext(sparkSession) }.asHadoop should be { outputDir / "parq-csv" }
      }

      "convert successfully to json" in {
        FileExportJob.create(
          { inputDir / "parq" }.asUriString,
          { outputDir / "parq-json" }.asUriString,
          ParquetFileFormat,
          JsonFileFormat,
          limit = None
        ).call { new TestExportJobContext(sparkSession) }.asHadoop should be { outputDir / "parq-json" }
      }
    }

  }

}

sealed case class User(firstName: String, lastName: String, age: Int, lastModified: Long)

object User {

  def random = apply(
    Random.alphanumeric.take { Random.nextInt(10) + 5 }.mkString,
    Random.alphanumeric.take { Random.nextInt(10) + 5 }.mkString,
    Random.nextInt(65) + 20,
    System.currentTimeMillis() - Random.nextInt(3600)
  )

}

class TestExportJobContext(sparkSession: SparkSession) extends JobContext {

  def sc() = sparkSession.sparkContext

  def sqlctx() = sparkSession.sqlContext

  def hivectx() = throw new UnsupportedOperationException("Hive Context is not supported in test")

  def streamingctx() = throw new UnsupportedOperationException("Streaming Context is not supported in test")

  def getSharedObject[E](s: String) = throw new UnsupportedOperationException("Get shared object is not supported in test")

  def setSharedObject[E](s: String, e: E) = throw new UnsupportedOperationException("Set shared object is not supported in test")

  def removeSharedObject[E](s: String) = throw new UnsupportedOperationException("Remove shared object is not supported in test")

  def createStreamingContext(l: Long) = throw new UnsupportedOperationException("Streaming is not supported in test")

  def stopStreamingCtx() = throw new UnsupportedOperationException("Remove shared object is not supported in test")

  def getLocalTmpDir = throw new UnsupportedOperationException("Local temp dir is not supported in test")

  def sparkSession[E]() = ???
}
