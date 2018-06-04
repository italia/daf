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

package daf.dataset.export.cleanup

import org.slf4j.Logger

sealed case class CleanupStatistics(successes: List[SuccessfulAttempt], failures: List[FailedAttempt], timeElapsed: Long) {

  lazy val fatalFailures    = failures.filter { _.reason.nonEmpty }

  lazy val nonFatalFailures = failures.filter { _.reason.isEmpty }

  private def logSuccesses(logger: Logger) = if (successes.nonEmpty) {
    logger.info { s"Successfully deleted [${successes.size}] path(s)" }
    successes.foreach {
      case SuccessfulAttempt(path) => logger.debug { s"${path.toString}" }
    }
  }

  private def logFailures(logger: Logger) = if (failures.nonEmpty) {
    logger.warn { s"Failed to deleted [${successes.size}] path(s)" }
    failures.foreach {
      case FailedAttempt(path, None)         => logger.warn { s"${path.toString} - reason unknown" }
      case FailedAttempt(path, Some(reason)) => logger.warn(s"${path.toString}", reason)
    }
  }

  def log(logger: Logger) = {
    logSuccesses(logger)
    logFailures(logger)
    logger.info { s"Cleanup finished in [$timeElapsed] millisecond(s)" }
  }

}

object CleanupStatistics {

  private def splitAttempts(attempts: List[CleanupAttempt],
                            successes: List[SuccessfulAttempt] = List.empty[SuccessfulAttempt],
                            failures: List[FailedAttempt] = List.empty[FailedAttempt]): (List[SuccessfulAttempt], List[FailedAttempt]) = attempts match {
    case (attempt: SuccessfulAttempt) :: tail => splitAttempts(tail, attempt  :: successes, failures)
    case (attempt: FailedAttempt)     :: tail => splitAttempts(tail, successes, attempt  :: failures)
    case Nil                                  => (successes, failures)
  }

  def collect(attempts: List[CleanupAttempt], timeElapsed: Long) = splitAttempts(attempts) match {
    case (successes, failures) => apply(successes, failures, timeElapsed)
  }

}
