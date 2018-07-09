package daf.dataset.query.jdbc

case class JdbcQueryAnalysis(memoryReservation: Double, memoryEstimation: Double, steps: Seq[ExecutionStep]) {

  def numSteps = steps.size

  def combine(other: JdbcQueryAnalysis) = this.copy(
    memoryReservation = math.max(this.memoryReservation, other.memoryReservation),
    memoryEstimation  = math.max(this.memoryEstimation, other.memoryEstimation),
    steps             = { this.steps ++ other.steps }.sortBy { _.index }
  )

}

object JdbcQueryAnalysis {

  private val reservationRegex = "^Per-Host Resource Reservation: Memory=(\\d+\\.?\\d{0,2})(KB|MB|GB)".r
  private val estimationRegex  = "^Per-Host Resource Estimates: Memory=(\\d+\\.?\\d{0,})(KB|MB|GB)".r

  private val stepRegex = "[|,\\-,\\s]{0,}(\\d+):(.*)".r

  private def toMB(value: Double, unit: String) = unit.toUpperCase match {
    case "MB" => value
    case "KB" => value / 1024
    case "GB" => value * 1024
    case other => throw new IllegalArgumentException(s"Unable to process memory unit [$other]")
  }


  def empty: JdbcQueryAnalysis = apply(0, 0, Seq.empty)

  def fromString(string: String): JdbcQueryAnalysis = string match {
    case stepRegex(index, text)        => apply(0, 0, Seq(ExecutionStep(index.toInt, text)))
    case estimationRegex(value, unit)  => apply(0, toMB(value.toDouble, unit), Seq.empty)
    case reservationRegex(value, unit) => apply(toMB(value.toDouble, unit), 0, Seq.empty)
    case _                             => empty
  }

}

sealed case class ExecutionStep(index: Int, text: String)
