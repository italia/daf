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

import cats.data.State

trait StateFunctions[A] {

  def set(f: A => Unit): State[A, Unit] = State[A, Unit] { s =>
    f.andThen { s -> _ } apply s
  }

  def get[B](f: A => B): State[A, B] = State[A, B] { s => s -> f(s) }

}
