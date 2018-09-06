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

import java.io.FileNotFoundException

import akka.actor.{ Actor, Props }
import config.FileExportConfig
import daf.filesystem.StringPathSyntax
import org.apache.hadoop.fs.{ FileStatus, FileSystem }
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success, Try }

/**
  * Akka Actor providing functionality for cleanup of stale export data. A cleanup is attempted first and when
  * successful, the next cleanup will be rescheduled to an amount of time determined by `pollInterval`. When an
  * attempt fails for catastrophic reasons, the Actor will attempt to do a new cleanup after an amount of time
  * determined by `backOffInterval`. If the cleanup fails for longer than `maxRetries`, then the actor will simply
  * throw an exception and go idle.
  *
  * At the end of each cleanup attempt, the Actor will log information on what files it managed to delete and which it
  * could not, detailing the reason why when possible.
  *
  * @note There is currently no guard for a growing list of files that cannot be deleted; white/black lists might be
  *       implemented to safeguard the Actor against such massive lists.
  *
  * @param pollInterval the amount of time to wait before cleaning the export path again after a successful attempt
  * @param backOffInterval the amount of time to wait before reattempting a failed cleanup attempt
  * @param maxAge the longest an exported directory is allowed to exist before it is cleaned
  * @param maxRetries the number of times a failed attempt is retried
  * @param exportPath the base path where the export results are stored
  * @param fileSystem the `FileSystem` instance used for interaction
  */
class FileExportCleanupActor(pollInterval: FiniteDuration,
                             backOffInterval: FiniteDuration,
                             maxAge: FiniteDuration,
                             maxRetries: Int,
                             exportPath: String,
                             fileSystem: FileSystem) extends Actor {

  implicit val executionContext = context.dispatcher

  private val logger = LoggerFactory.getLogger("it.gov.daf.ExportCleaner")

  private val maxAgeMillis = maxAge.toMillis

  private def collect(timestamp: Long) = Try { fileSystem.listStatus(exportPath.asHadoop) }.recover {
    case _: FileNotFoundException => Array.empty[FileStatus]
  }.map {
    _.toList.filter { status => timestamp - status.getModificationTime > maxAgeMillis }
  }

  private def attemptClean(dir: FileStatus): CleanupAttempt = Try { fileSystem.delete(dir.getPath, true) } match {
    case Success(true)   => SuccessfulAttempt(dir.getPath)
    case Success(false)  => FailedAttempt(dir.getPath)
    case Failure(reason) => FailedAttempt(dir.getPath, Some(reason))
  }

  private def attemptClean(dirs: List[FileStatus]): Try[List[CleanupAttempt]] = Try {
    dirs.map { attemptClean }
  }

  private def collectStatistics(attempts: List[CleanupAttempt], timestamp: Long) = Try {
    CleanupStatistics.collect(attempts, System.currentTimeMillis - timestamp)
  }

  private def doClean(timestamp: Long) = for {
    dirs       <- collect(timestamp)
    attempts   <- attemptClean(dirs)
    statistics <- collectStatistics(attempts, timestamp)
  } yield statistics.log(logger)

  private def doSchedule() = context.system.scheduler.scheduleOnce(pollInterval) { self ! DoClean.now }

  private def doScheduleError(originalTimestamp: Long, attempt: Int, error: Throwable) = if (attempt < maxRetries + 1) {
    logger.error(s"A critical error occurred while attempting cleanup; backing off for [${backOffInterval.toSeconds}] second(s)", error)
    context.system.scheduler.scheduleOnce(backOffInterval) { self ! DoClean(originalTimestamp, attempt + 1) }
  } else {
    throw new RuntimeException(s"Maximum number of retries exhausted: cleanup failed [$attempt] time(s)", error)
  }

  private def handleClean(timestamp: Long, attempt: Int) = doClean(timestamp) match {
    case Success(_)     => doSchedule()
    case Failure(error) => doScheduleError(timestamp, attempt, error)
  }

  override def preStart(): Unit = doSchedule()

  def receive = {
    case DoClean(timestamp, attempt) if attempt < maxRetries + 1 => handleClean(timestamp, attempt)
    case DoClean(_, attempt)                                     => throw new RuntimeException(s"Maximum number of retries exhausted: cleanup failed [$attempt] time(s)")

  }

}

object FileExportCleanupActor {

  def props(exportServiceConfig: FileExportConfig)(implicit fileSystem: FileSystem): Props = props(
    exportServiceConfig.cleanup.pollInterval,
    exportServiceConfig.cleanup.backOffInterval,
    exportServiceConfig.cleanup.maxAge,
    exportServiceConfig.cleanup.maxRetries,
    exportServiceConfig.exportPath
  )

  def props(pollInterval: FiniteDuration,
            backOffInterval: FiniteDuration,
            maxAge: FiniteDuration,
            maxRetries: Int,
            exportPath: String)(implicit fileSystem: FileSystem): Props = Props {
    new FileExportCleanupActor(
      pollInterval,
      backOffInterval,
      maxAge,
      maxRetries,
      exportPath,
      fileSystem
    )
  }

}

sealed trait CleanupMessage

sealed case class DoClean(timestamp: Long, attempt: Int = 1) extends CleanupMessage

object DoClean {

  def now: DoClean = apply { System.currentTimeMillis }

}
