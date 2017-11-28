package it.teamdigitale

import scala.util.{Failure, Success, Try}

/**
  * This object validates
  */
object AggregationsValidator {

  val functions = Set("count", "max", "mean", "min", "sum")

  /**
    *
    * @param expression a valid aggregation function
    * @return the expression if it is valid or an failure otherwise
    */
  def validate(expression: String): Try[String] = {
    if (functions.contains(expression)) Success(expression)
    else Failure(new IllegalArgumentException(s"$expression is not a supported aggregation function in $functions"))
  }

}
