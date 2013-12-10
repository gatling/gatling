/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.check

import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation, noneSuccess }

object Validator {
	val foundNothingFailure = "found nothing".failure
}

trait Validator[A] {
	def name: String
	def apply(actual: Option[A]): Validation[Option[A]]
}

abstract class Matcher[A, E] extends Validator[A] {
	def expected: E
	def doMatch(actual: Option[A]): Validation[Option[A]]
	def apply(actual: Option[A]): Validation[Option[A]] =
		for {
			matchResult <- doMatch(actual).mapError(message => s"($expected) failed: $message")
		} yield matchResult
}

class IsMatcher[E](val expected: E) extends Matcher[E, E] {

	val name = "is"

	def doMatch(actual: Option[E]): Validation[Option[E]] = actual match {
		case Some(actualValue) =>
			if (actualValue == expected)
				actual.success
			else
				s"found $actualValue".failure
		case None => Validator.foundNothingFailure
	}
}

class NotMatcher[E](val expected: E) extends Matcher[E, E] {

	val name = "not"

	def doMatch(actual: Option[E]): Validation[Option[E]] = actual match {
		case Some(actualValue) =>
			if (actualValue != expected)
				actual.success
			else
				s"unexpectedly found $actualValue".failure
		case None => noneSuccess
	}
}

class InMatcher[E](val expected: Seq[E]) extends Matcher[E, Seq[E]] {

	val name = "in"

	def doMatch(actual: Option[E]): Validation[Option[E]] = actual match {
		case Some(actualValue) =>
			if (expected.contains(actualValue))
				actual.success
			else
				s"found $actualValue".failure
		case None => Validator.foundNothingFailure
	}
}

class CompareMatcher[E](val name: String, message: String, compare: (E, E) => Boolean, val expected: E) extends Matcher[E, E] {

	def doMatch(actual: Option[E]): Validation[Option[E]] = actual match {
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
		case None => Validator.foundNothingFailure
	}
}

class NotExistsValidator[A] extends Validator[A] {
	val name = "notExists"
	def apply(actual: Option[A]): Validation[Option[A]] = actual match {
		case Some(actualValue) => s"unexpectedly found $actualValue".failure
		case None => noneSuccess
	}
}

class NoopValidator[A] extends Validator[A] {
	val name = "noop"
	def apply(actual: Option[A]): Validation[Option[A]] = actual.success
}
