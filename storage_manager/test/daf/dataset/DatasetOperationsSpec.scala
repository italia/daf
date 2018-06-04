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

package daf.dataset

import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.util.Try

class DatasetOperationsSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val spark = SparkSession.builder()
    .appName("test")
    .master("local[1]")
    .getOrCreate()

  val path = this.getClass.getResource("/employees.json").toExternalForm
  val employees = spark.read.json(path).toDF()
  employees.show(10)

  "A DatasetOperationsSpec" should "select a column in a dataset" in {
    val result = DatasetOperations.select(employees, "salary")
    result shouldBe 'Success
    result.get.count() should be > 0L
    result.foreach(_.show())
  }

  it should "select on multiple columns" in {
    val result = DatasetOperations.select(employees, List("salary", "type"))
    result shouldBe 'Success
    result.get.count() should be > 0L
    result.foreach(_.show())
  }

  it should "return an error if a non valid column is selected" in {
    val result = DatasetOperations.select(employees, "salary2")
    result shouldBe 'Failure
  }

  it should "return data for a valid where condition" in {
    val result = DatasetOperations.where(employees, List("salary > 1000"))
    result shouldBe 'Success
    result.get.count() === 2L
    result.foreach(_.show())
  }

  it should "return an error for a invalid where condition" in {
    val result = DatasetOperations.where(employees, List("salary * 1000"))
    result shouldBe 'Failure
  }

  it should "aggregate correctly for column -> count" in {
    val result = DatasetOperations.groupBy(employees, "type", "salary" -> "count")
    result shouldBe 'Success
    result.foreach(_.show())
  }

  it should "aggregate correctly for multiple conditions" in {
    // Selects the age of the oldest employee and then aggregate salary for each type payment
    val result = DatasetOperations.groupBy(
      df = employees,
      column = "type",
      groupByOps = "age" -> "max", "salary" -> "mean")

    result shouldBe 'Success
    result.foreach(_.show())
  }

  it should "return an error for a invalid groupBy condition" in {
    val result = DatasetOperations.groupBy(employees, "type", "salay" -> "avg")
    result shouldBe 'Failure
  }

  it should "display only 2 record if limited to 2" in {
    val result = DatasetOperations.limit(employees, 2)
    result shouldBe 'Success
    result.get.count() === 2L
    result.foreach(_.show())
  }
}
