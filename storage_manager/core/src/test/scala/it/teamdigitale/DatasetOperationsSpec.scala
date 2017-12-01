package it.teamdigitale

import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.util.Try

class DatasetOperationsSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val spark = SparkSession.builder()
    .appName("test")
    .master("local[1]")
    .getOrCreate()

  import spark.implicits._

  val path = this.getClass.getResource("/employees.json").toExternalForm
  val employees = spark.read.json(path).toDF()
  employees.show(10)

  "A DatasetOperationsSpec" should "select a column in a dataset" in {
    val result = DatasetOperations.select(Try(employees), "salary")
    result shouldBe 'Success
    result.get.count() should be > 0L
    result.foreach(_.show())
  }

  it should "select on multiple columns" in {
    val result = DatasetOperations.select(Try(employees), List("salary", "type"))
    result shouldBe 'Success
    result.get.count() should be > 0L
    result.foreach(_.show())
  }

  it should "return an error if a non valid column is selected" in {
    val result = DatasetOperations.select(Try(employees), "salary2")
    result shouldBe 'Failure
  }

  it should "return data for a valid where condition" in {
    val result = DatasetOperations.where(Try(employees), List("salary > 1000"))
    result shouldBe 'Success
    result.get.count() === 2L
    result.foreach(_.show())
  }

  it should "return an error for a invalid where condition" in {
    val result = DatasetOperations.where(Try(employees), List("salary * 1000"))
    result shouldBe 'Failure
  }

  it should "aggregate correctly for column -> count" in {
    val result = DatasetOperations.groupBy(Try(employees), "type", "salary" -> "count")
    result shouldBe 'Success
    result.foreach(_.show())
  }

  it should "aggregate correctly for multiple conditions" in {
    // Selects the age of the oldest employee and then aggregate salary for each type payment
    val result = DatasetOperations.groupBy(
      df = Try(employees),
      column = "type",
      groupByOps = "age" -> "max", "salary" -> "mean")

    result shouldBe 'Success
    result.foreach(_.show())
  }

  it should "return an error for a invalid groupBy condition" in {
    val result = DatasetOperations.groupBy(Try(employees), "type", "salay" -> "avg")
    result shouldBe 'Failure
  }

  it should "display only 2 record if limited to 2" in {
    val result = DatasetOperations.limit(Try(employees), 2)
    result shouldBe 'Success
    result.get.count() === 2L
    result.foreach(_.show())
  }
}
