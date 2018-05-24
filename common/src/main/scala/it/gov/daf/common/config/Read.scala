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

package it.gov.daf.common.config

import play.api.Configuration

import scala.concurrent.duration._

object Read {

  def config(key: String): OptionalConfigReader[Configuration] = OptionalConfigReader(key) { _.getConfig(key) }

  def int(key: String): OptionalConfigReader[Int] = OptionalConfigReader(key) { _.getInt(key) }

  def long(key: String): OptionalConfigReader[Long] = OptionalConfigReader(key) { _.getLong(key) }

  def string(key: String): OptionalConfigReader[String] = OptionalConfigReader(key) { _.getString(key) }

  def double(key: String): OptionalConfigReader[Double] = OptionalConfigReader(key) { _.getDouble(key) }

  def boolean(key: String): OptionalConfigReader[Boolean] = OptionalConfigReader(key) { _.getBoolean(key) }

  def time(key: String): OptionalConfigReader[FiniteDuration] = OptionalConfigReader(key) {
    _.getMilliseconds(key).map { _.milliseconds }
  }

}
