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

import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

object Matcher {

	def is[X] = new Matcher[X, X] {
		def apply(actual: Option[X], expected: X): Validation[Option[X]] = actual match {
			case Some(actualValue) =>
				if (actualValue == expected)
					actual.success
				else
					s"found $actualValue".failure
			case None => "found nothing".failure
		}
		val name = "is"
	}

	def not[X] = new Matcher[X, X] {
		def apply(actual: Option[X], expected: X): Validation[Option[X]] = actual match {
			case Some(actualValue) =>
				if (actualValue != expected)
					actual.success
				else
					s"unexpectedly found $actualValue".failure
			case None => None.success
		}
		val name = "not"
	}

	def in[X] = new Matcher[X, Seq[X]] {
		def apply(actual: Option[X], expected: Seq[X]): Validation[Option[X]] = actual match {
			case Some(actualValue) =>
				if (expected.contains(actualValue))
					actual.success
				else
					s"found $actualValue".failure
			case None => "found nothing".failure
		}
		val name = "in"
	}

	def lessThan[X: Ordering] = compare("lessThan", "less than", implicitly[Ordering[X]].lt)
	def lessThanOrEqual[X: Ordering] = compare("lessThanOrEqual", "less than or equal to", implicitly[Ordering[X]].lteq)
	def greaterThan[X: Ordering] = compare("greaterThan", "greater than", implicitly[Ordering[X]].gt)
	def greaterThanOrEqual[X: Ordering] = compare("greaterThanOrEqual", "greater than or equal to", implicitly[Ordering[X]].gteq)
	def compare[X: Ordering](compareName: String, message: String, comp: (X, X) => Boolean) = new Matcher[X, X] {
		def apply(actual: Option[X], expected: X): Validation[Option[X]] = actual.map { actualValue =>
			if (comp(actualValue, expected))
				actual.success
			else
				s"$actualValue is not $message $expected".failure

		}.getOrElse(s"can't compare nothing and $expected".failure)
		val name = compareName
	}

	def exists[X] = new Matcher[X, String] {
		def apply(actual: Option[X], expected: String): Validation[Option[X]] = actual match {
			case Some(actualValue) => actual.success
			case None => "found nothing".failure
		}
		val name = "exists"
	}

	def notExists[X] = new Matcher[X, String] {
		def apply(actual: Option[X], expected: String): Validation[Option[X]] = actual match {
			case Some(actualValue) => s"unexpectedly found $actualValue".failure
			case None => None.success
		}
		val name = "notExists"
	}

	def whatever[X] = new Matcher[X, String] {
		def apply(actual: Option[X], expected: String): Validation[Option[X]] = actual.success
		val name = "whatever"
	}
}

trait Matcher[A, E] {
	def apply(actual: Option[A], expected: E): Validation[Option[A]]
	def name: String
}
