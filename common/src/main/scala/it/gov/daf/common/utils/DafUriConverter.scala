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

package it.gov.daf.common.utils

sealed trait DatasetType
case object Standard extends DatasetType
case object Ordinary extends DatasetType
case object OpenData extends DatasetType

class DafUriConverter(
  dsType: DatasetType,
  organization: String,
  theme: String,
  subTheme: String,
  dsName: String
) {

  private val logicalUri = dsType match {
    case Standard =>
      s"daf://dataset/standard/$theme" + "__" + s"$subTheme/$dsName"
    case Ordinary =>
      s"daf://dataset/$organization/$theme" + "__" + s"$subTheme/$dsName"
    case OpenData =>
      s"daf://opendata/$dsName"
  }

  private val physicalUri = dsType match {
    case Standard =>
      s"/daf/standard/$theme" + "__" + s"$subTheme/$dsName"
    case Ordinary =>
      s"/daf/ordinary/$organization/$theme" + "__" + s"$subTheme/$dsName"
    case OpenData =>
      s"/daf/opendata/$dsName"
  }

  def toLogicalUri: String = logicalUri

  def toPhysicalUri(): String = physicalUri

}
