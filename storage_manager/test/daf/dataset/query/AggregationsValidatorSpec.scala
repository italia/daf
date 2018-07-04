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

package daf.dataset.query

import org.scalatest.{ FlatSpec, Matchers }

import scala.util.Success

class AggregationsValidatorSpec extends FlatSpec with Matchers {

  val validFunctions = AggregationsValidator.functions

  "AggregationsValidator" should  s"return success for $validFunctions" in {
    validFunctions.foreach { f => AggregationsValidator.validate(f) shouldBe Success(f) }
  }

  it should "return an error for `Sum`" in {
    AggregationsValidator.validate("Sum") should not be Success("Sum")
  }

  it should "return an error for `Test`" in {
    AggregationsValidator.validate("Test") should not be Success("Test")
  }

}
