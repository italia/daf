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

package daf.filesystem

trait FileFormat {

  def extensions: Set[String]

  def compressionRatio: Double

}

trait SingleExtension { this: FileFormat =>

  protected def extension: String

  final lazy val extensions = Set(extension)

}

trait NoExtensions { this: FileFormat =>

  final def extensions = Set.empty[String]

}

trait NoCompression { this: FileFormat =>

  final lazy val compressionRatio = 1.0

}