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

package io.gatling.core.check

import io.gatling.commons.util.Equality
import io.gatling.commons.validation._

object Validator {
  val FoundNothingFailure: Failure = "found nothing".failure
}

trait Validator[A] {
  def name: String
  def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]]
}

abstract class Matcher[A] extends Validator[A] {
  protected def doMatch(actual: Option[A]): Validation[Option[A]]
  def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] =
    if (displayActualValue) {
      doMatch(actual).mapError(message => s"but actually $message")
    } else {
      doMatch(actual)
    }
}

class IsMatcher[A](expected: A, equality: Equality[A]) extends Matcher[A] {

  def name = s"is($expected)"

  protected def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) =>
      if (equality.equals(actualValue, expected)) {
        actual.success
      } else {
        s"found $actualValue".failure
      }
    case _ => Validator.FoundNothingFailure
  }
}

class IsNullMatcher[A] extends Matcher[A] {

  def name = "isNull"

  protected def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) =>
      if (actualValue == null) {
        actual.success
      } else {
        s"found $actualValue".failure
      }
    case _ => Validator.FoundNothingFailure
  }
}

class NotMatcher[A](expected: A, equality: Equality[A]) extends Matcher[A] {

  def name = s"not($expected)"

  protected def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) =>
      if (!equality.equals(actualValue, expected)) {
        actual.success
      } else {
        s"unexpectedly found $actualValue".failure
      }
    case _ => NoneSuccess
  }
}

class NotNullMatcher[A] extends Matcher[A] {

  def name = "notNull"

  protected def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) =>
      if (actualValue != null)
        actual.success
      else
        "found null".failure
    case _ => Validator.FoundNothingFailure
  }
}

class InMatcher[A](expected: Seq[A]) extends Matcher[A] {

  def name: String = expected.mkString("in(", ",", ")")

  protected def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
    case Some(actualValue) =>
      if (expected.contains(actualValue))
        actual.success
      else
        s"found $actualValue".failure
    case _ => Validator.FoundNothingFailure
  }
}

class CompareMatcher[A](val comparisonName: String, message: String, compare: (A, A) => Boolean, expected: A) extends Matcher[A] {

  def name = s"$comparisonName($expected)"

  protected def doMatch(actual: Option[A]): Validation[Option[A]] = actual match {
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
  def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
    case None => Validator.FoundNothingFailure
    case _    => actual.success
  }
}

class NotExistsValidator[A] extends Validator[A] {
  val name = "notExists"
  def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
    case Some(actualValue) => s"unexpectedly found $actualValue".failure
    case _                 => NoneSuccess
  }
}

class NoopValidator[A] extends Validator[A] {
  val name = "noop"
  def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual.success
}
