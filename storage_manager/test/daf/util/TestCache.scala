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

package daf.util

import play.api.cache.CacheApi

import scala.collection.mutable.{ Map => MutableMap }
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class TestCache extends CacheApi {

  private val cache = MutableMap.empty[String, Any]

  def set(key: String, value: Any, expiration: Duration) = cache += { key -> value }

  def remove(key: String) = cache -= key

  def getOrElse[A](key: String, expiration: Duration)(orElse: => A)(implicit A: ClassTag[A]) = cache.get(key) match {
    case Some(value: A) => value
    case None           => orElse
    case Some(value)    => throw new IllegalArgumentException(s"Value found for key [$key] had type [${value.getClass.getSimpleName}] instead of the expected [${A.runtimeClass.getSimpleName}]")
  }

  def get[A](key: String)(implicit A: ClassTag[A]) = cache.get(key) match {
    case Some(value: A) => Some(value)
    case None           => None
    case Some(value)    => throw new IllegalArgumentException(s"Value found for key [$key] had type [${value.getClass.getSimpleName}] instead of the expected [${A.runtimeClass.getSimpleName}]")
  }

}
