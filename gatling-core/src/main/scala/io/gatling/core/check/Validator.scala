/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.core.check

import io.gatling.commons.validation._

object Validator {
  val FoundNothingFailure = "found nothing".failure
}

trait Validator[A] {
  def name: String
  def apply(actual: Option[A]): Validation[Option[A]]
}

abstract class Matcher[A] extends Validator[A] {
  def doMatch(actual: Option[A]): Validation[Option[A]]
  def apply(actual: Option[A]): Validation[Option[A]] =
    doMatch(actual).mapError(message => s"but actually $message")
}

class IsMatcher[A](expected: A) extends Matcher[A] {

  def name = s"is($expected)"

  def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) =>
      if (actualValue == expected)
        actual.success
      else
        s"found $actualValue".failure
    case None => Validator.FoundNothingFailure
  }
}

class NotMatcher[A](expected: A) extends Matcher[A] {

  def name = s"not($expected)"

  def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) =>
      if (actualValue != expected)
        actual.success
      else
        s"unexpectedly found $actualValue".failure
    case None => NoneSuccess
  }
}

class InMatcher[A](expected: Seq[A]) extends Matcher[A] {

  def name = expected.mkString("in(", ",", ")")

  def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) =>
      if (expected.contains(actualValue))
        actual.success
      else
        s"found $actualValue".failure
    case None => Validator.FoundNothingFailure
  }
}

class CompareMatcher[A](val comparisonName: String, message: String, compare: (A, A) => Boolean, expected: A) extends Matcher[A] {

  def name = s"$comparisonName($expected)"

  def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) =>
      if (compare(actualValue, expected))
        actual.success
      else
        s"$actualValue is not $message $expected".failure

    case _ => s"can't compare nothing and $expected".failure
  }
}

class ExistsValidator[A] extends Validator[A] {
  val name = "exists"
  def apply(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) => actual.success
    case None              => Validator.FoundNothingFailure
  }
}

class NotExistsValidator[A] extends Validator[A] {
  val name = "notExists"
  def apply(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) => s"unexpectedly found $actualValue".failure
    case None              => NoneSuccess
  }
}

class NoopValidator[A] extends Validator[A] {
  val name = "noop"
  def apply(actual: Option[A]): Validation[Option[A]] = actual.success
}
