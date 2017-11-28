package it.teamdigitale

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Success


class AggregationsValidatorSpec  extends FlatSpec with Matchers {

  val validFunctions = AggregationsValidator.functions

  "Am AggregationsValidator" should  s"return success for $validFunctions" in {
    validFunctions.map{ f =>
      AggregationsValidator.validate(f) shouldBe Success(f)
    }
  }

  it should "return an error for `Sum`" in {
    val res = AggregationsValidator.validate("Sum")
    println(res)
    res shouldBe 'Failure
  }

  it should "return an error for `Test`" in {
    val res = AggregationsValidator.validate("Test")
    println(res)
    res shouldBe 'Failure
  }

}
