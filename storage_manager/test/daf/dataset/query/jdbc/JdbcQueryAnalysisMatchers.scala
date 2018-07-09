package daf.dataset.query.jdbc

import org.scalatest.matchers.{ HavePropertyMatchResult, HavePropertyMatcher }

object JdbcQueryAnalysisMatchers {

  def memoryReservation(value: Double) = new HavePropertyMatcher[JdbcQueryAnalysis, Double] {
    def apply(a: JdbcQueryAnalysis) = HavePropertyMatchResult(
      a.memoryReservation == value,
      "memoryReservation",
      value,
      a.memoryReservation
    )
  }

  def memoryEstimate(value: Double) = new HavePropertyMatcher[JdbcQueryAnalysis, Double] {
    def apply(a: JdbcQueryAnalysis) = HavePropertyMatchResult(
      a.memoryEstimation == value,
      "memoryEstimation",
      value,
      a.memoryEstimation
    )
  }

  def numSteps(value: Int) = new HavePropertyMatcher[JdbcQueryAnalysis, Int] {
    def apply(a: JdbcQueryAnalysis) = HavePropertyMatchResult(
      a.numSteps == value,
      "numSteps",
      value,
      a.numSteps
    )
  }

}
