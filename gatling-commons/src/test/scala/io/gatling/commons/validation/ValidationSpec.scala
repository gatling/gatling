/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.commons.validation

import java.util.Locale

import io.gatling.BaseSpec

class ValidationSpec extends BaseSpec {

  "SuccessWrapper" should "wrap a value in a Success" in {
    1.success shouldBe Success(1)
  }

  "FailureWrapper" should "wrap a String in a Failure" in {
    "foo".failure shouldBe Failure("foo")
  }

  "map" should "apply the passed function to the value when called on a Success" in {
    1.success.map(_ + 1) shouldBe Success(2)
  }

  it should "return the current instance when called on a Failure" in {
    val failure: Validation[Int] = "foo".failure
    failure.map(_ + 1) should be theSameInstanceAs failure
  }

  "flatMap" should "called the passed function when called on a Success" in {
    1.success.flatMap(x => Success(x + 4)) shouldBe Success(5)
  }

  it should "return the current instance when called on Failure" in {
    val failure: Validation[String] = "foo".failure
    failure.flatMap(x => Success(x.toUpperCase(Locale.ROOT))) should be theSameInstanceAs failure
  }
  "mapError" should "return the current instance when called on a Success" in {
    val success = 1.success
    success.mapError(_.toUpperCase(Locale.ROOT)) should be theSameInstanceAs success
  }

  it should "apply the passed function on the error message when called on a Failure" in {
    "foo".failure.mapError(_.toUpperCase(Locale.ROOT)) shouldBe Failure("FOO")
  }

  "filter" should "return the current instance when called on a Success and the predicate holds" in {
    val success = 1.success
    success.filter(_ == 1) should be theSameInstanceAs success
  }

  it should "return a Failure when called on a Success and the predicate does not hold" in {
    val success = 1.success
    success.filter(_ == 2) shouldBe a[Failure]
  }

  it should "return the current instance when called on a Failure" in {
    val failure: Validation[Int] = "foo".failure
    failure.filter(_ % 2 == 0) should be theSameInstanceAs failure
  }

  "withFilter" should "have the same behaviour as filter" in {
    val success = 1.success
    success.withFilter(_ == 1) should be theSameInstanceAs success

    success.withFilter(_ == 2) shouldBe a[Failure]

    val failure: Validation[Int] = "foo".failure
    failure.withFilter(_ % 2 == 0) should be theSameInstanceAs failure
  }

  "onSuccess" should "call the passed function when called on a Success" in {
    var i = 0
    1.success.onSuccess(_ => i += 1)
    i shouldBe 1
  }

  it should "do nothing when called on a Failure" in {
    var i = 0
    "foo".failure.onSuccess(_ => i += 1)
    i shouldBe 0
  }

  "foreach" should "have the same behaviour as onSuccess" in {
    var i = 0
    1.success.foreach(_ => i += 1)
    i shouldBe 1

    i = 0
    "foo".failure.foreach(_ => i += 1)
    i shouldBe 0
  }

  "onFailure" should "do nothing when called on a Success" in {
    var i = 0
    1.success.onFailure(_ => i += 1)
    i shouldBe 0
  }

  it should "call the passed function called on a Failure" in {
    var i = 0
    "foo".failure.onFailure(_ => i += 1)
    i shouldBe 1
  }

  "recover" should "return the current instance when called on a Success" in {
    1.success.recover(4) shouldBe Success(1)
  }

  it should "return the passed value wrapped in a Success when called on a Failure" in {
    "foo".failure.recover(4) shouldBe Success(4)
  }

  "Option.toValidation" should "resolve Some to a Success" in {
    Some("foo").toValidation("error") shouldBe Success("foo")
  }

  it should "resolve None to a Failure" in {
    None.toValidation("error") shouldBe Failure("error")
  }
}
