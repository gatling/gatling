/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

trait Validator[A] {
  def name: String
  def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]]
}

object Validator {
  val FoundNothingFailure: Failure = "found nothing".failure
  val GenericFailure: Failure = "failed".failure

  final class Exists[A] extends Validator[A] {
    val name = "exists"
    def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
      case None => Validator.FoundNothingFailure
      case _    => actual.success
    }
  }

  final class NotExists[A] extends Validator[A] {
    val name = "notExists"
    def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
      case Some(actualValue) =>
        if (displayActualValue) {
          s"unexpectedly found $actualValue".failure
        } else {
          Validator.GenericFailure
        }
      case _ => Validation.NoneSuccess
    }
  }

  final class Optional[A] extends Validator[A] {
    val name = "optional"
    def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual.success
  }
}

object Matcher {
  final class Is[A](expected: A, equality: Equality[A]) extends Validator[A] {
    def name = s"is($expected)"

    override def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
      case Some(actualValue) =>
        if (equality.equals(actualValue, expected)) {
          actual.success
        } else if (displayActualValue) {
          s"found $actualValue".failure
        } else {
          Validator.GenericFailure
        }
      case _ => Validator.FoundNothingFailure
    }
  }

  final class IsNull[A] extends Validator[A] {
    def name = "isNull"

    override def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
      case Some(actualValue) =>
        if (actualValue == null) {
          actual.success
        } else if (displayActualValue) {
          s"found $actualValue".failure
        } else {
          Validator.GenericFailure
        }
      case _ => Validator.FoundNothingFailure
    }
  }

  final class Not[A](expected: A, equality: Equality[A]) extends Validator[A] {
    def name = s"not($expected)"

    override def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
      case Some(actualValue) =>
        if (equality.equals(actualValue, expected)) {
          s"unexpectedly found $actualValue".failure
        } else if (displayActualValue) {
          actual.success
        } else {
          Validator.GenericFailure
        }
      case _ => Validation.NoneSuccess
    }
  }

  final class NotNull[A] extends Validator[A] {
    def name = "notNull"

    override def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
      case Some(actualValue) =>
        if (actualValue == null) {
          Validator.GenericFailure
        } else {
          actual.success
        }
      case _ => Validator.FoundNothingFailure
    }
  }

  final class In[A](expected: Seq[A]) extends Validator[A] {
    def name: String = expected match {
      case range: Range => s"in(${range.toString})"
      case _            => expected.mkString("in(", ",", ")")
    }

    override def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
      case Some(actualValue) =>
        if (expected.contains(actualValue)) {
          actual.success
        } else if (displayActualValue) {
          s"found $actualValue".failure
        } else {
          Validator.GenericFailure
        }
      case _ => Validator.FoundNothingFailure
    }
  }

  final class Compare[A](val comparisonName: String, message: String, compare: (A, A) => Boolean, expected: A) extends Validator[A] {
    def name = s"$comparisonName($expected)"

    override def apply(actual: Option[A], displayActualValue: Boolean): Validation[Option[A]] = actual match {
      case Some(actualValue) =>
        if (compare(actualValue, expected)) {
          actual.success
        } else if (displayActualValue) {
          s"$actualValue is not $message $expected".failure
        } else {
          Validator.GenericFailure
        }

      case _ => s"can't compare nothing and $expected".failure
    }
  }
}
