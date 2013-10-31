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

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

object Validator {
	val foundNothingFailure = "found nothing".failure
}

trait Validator[A] {
	def name: String
	def apply(session: Session, actual: Option[A]): Validation[Option[A]]
}

abstract class Matcher[A, E] extends Validator[A] {
	def expected: Expression[E]
	def doMatch(actual: Option[A], expected: E): Validation[Option[A]]
	def apply(session: Session, actual: Option[A]): Validation[Option[A]] =
		for {
			expected <- expected(session).mapError(message => s"could not resolve expected value: $message")
			matchResult <- doMatch(actual, expected).mapError(message => s"($expected) didn't match: $message")
		} yield matchResult
}

class IsMatcher[X](val expected: Expression[X]) extends Matcher[X, X] {

	val name = "is"

	def doMatch(actual: Option[X], expected: X): Validation[Option[X]] = actual match {
		case Some(actualValue) =>
			if (actualValue == expected)
				actual.success
			else
				s"found $actualValue".failure
		case None => Validator.foundNothingFailure
	}
}

class NotMatcher[X](val expected: Expression[X]) extends Matcher[X, X] {

	val name = "not"

	def doMatch(actual: Option[X], expected: X): Validation[Option[X]] = actual match {
		case Some(actualValue) =>
			if (actualValue != expected)
				actual.success
			else
				s"unexpectedly found $actualValue".failure
		case None => None.success
	}
}

class InMatcher[X](val expected: Expression[Seq[X]]) extends Matcher[X, Seq[X]] {

	val name = "in"

	def doMatch(actual: Option[X], expected: Seq[X]): Validation[Option[X]] = actual match {
		case Some(actualValue) =>
			if (expected.contains(actualValue))
				actual.success
			else
				s"found $actualValue".failure
		case None => Validator.foundNothingFailure
	}
}

class CompareMatcher[X: Ordering](val name: String, message: String, comp: (X, X) => Boolean, val expected: Expression[X]) extends Matcher[X, X] {

	def doMatch(actual: Option[X], expected: X): Validation[Option[X]] = actual.map { actualValue =>
		if (comp(actualValue, expected))
			actual.success
		else
			s"$actualValue is not $message $expected".failure

	}.getOrElse(s"can't compare nothing and $expected".failure)
}

class ExistsValidator[X] extends Validator[X] {
	val name = "exists"
	def apply(session: Session, actual: Option[X]): Validation[Option[X]] = actual match {
		case Some(actualValue) => actual.success
		case None => Validator.foundNothingFailure
	}
}

class NotExistsValidator[X] extends Validator[X] {
	val name = "notExists"
	def apply(session: Session, actual: Option[X]): Validation[Option[X]] = actual match {
		case Some(actualValue) => s"unexpectedly found $actualValue".failure
		case None => None.success
	}
}

class WhateverValidator[X] extends Validator[X] {
	val name = "whatever"
	def apply(session: Session, actual: Option[X]): Validation[Option[X]] = actual.success
}
