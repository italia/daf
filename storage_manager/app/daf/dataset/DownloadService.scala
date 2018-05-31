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

import java.io.InputStream

import daf.filesystem.{ DirectoryInfo, FileInfo, MergeStrategies, PathInfo, StringPathSyntax }
import org.apache.hadoop.fs.FileSystem

import scala.util.{ Failure, Success, Try }

/**
  * Service that allows interaction with the given file system for any operations aimed at facilitating downloads.
  * @param fileSystem the [[FileSystem]] instance to have the service interact with.
  */
class DownloadService(implicit fileSystem: FileSystem) {

  private def openFiles(files: Seq[FileInfo]) = Try {
    files.map { file => fileSystem.open(file.path) }
  }

  private def checkCompression(directory: DirectoryInfo) =
    if (directory.hasMixedCompressions) Failure { new IllegalArgumentException(s"Directory [${directory.path.getName}] has files with mixed compression formats") }
    else Success { directory }

  private def checkFormats(directory: DirectoryInfo) =
    if (directory.hasMixedFormats) Failure { new IllegalArgumentException(s"Directory [${directory.path.getName}] has files with mixed data formats") }
    else Success { directory }

  private def checkNonEmpty(directory: DirectoryInfo) =
    if (directory.isEmpty) Failure { new IllegalArgumentException(s"Directory [${directory.path.getName}] is empty") }
    else Success { directory }

  private def findMergeStrategy(directory: DirectoryInfo) = Try {
    directory.files.headOption.map { MergeStrategies.find } getOrElse MergeStrategies.default
  }

  def info(path: String) = Try { PathInfo.fromHadoop(path.asHadoop) }

  def open(directory: DirectoryInfo): Try[InputStream] = for {
    nonEmptyChecked    <- checkNonEmpty(directory)
    compressionChecked <- checkCompression(nonEmptyChecked)
    formatChecked      <- checkFormats(compressionChecked)
    mergeStrategy      <- findMergeStrategy(formatChecked)
    inputStreams       <- openFiles(formatChecked.files)
  } yield mergeStrategy.merge(inputStreams)


  def open(file: FileInfo): Try[InputStream] = Try { fileSystem.open(file.path) }

  def open(path: String): Try[InputStream] = info(path).flatMap {
    case directory: DirectoryInfo => open(directory)
    case file: FileInfo           => open(file)
  }


}