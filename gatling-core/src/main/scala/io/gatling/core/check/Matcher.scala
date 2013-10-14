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
		def name: String = "is"
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
		def name: String = "not"
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
		def name: String = "in"
	}

	class Comparer[X](val name: String, message: String, comp: (X, X) => Boolean) extends Matcher[X, X] {

		def apply(actual: Option[X], expected: X): Validation[Option[X]] =
			actual.map { actualValue =>
				if (comp(actualValue, expected))
					actual.success
				else
					s"$actualValue is not $message $expected".failure

			}.getOrElse(s"can't compare nothing and $expected".failure)
	}

	def lessThan[X](implicit ordering: Ordering[X]) = new Comparer[X]("lessThan", "less than", ordering.lt)
	def lessThanOrEqual[X](implicit ordering: Ordering[X]) = new Comparer[X]("lessThanOrEqual", "less than or equal to", ordering.lteq)
	def greaterThan[X](implicit ordering: Ordering[X]) = new Comparer[X]("greaterThan", "greater than", ordering.gt)
	def greaterThanOrEqual[X](implicit ordering: Ordering[X]) = new Comparer[X]("greaterThanOrEqual", "greater than or equal to", ordering.gteq)

	def exists[X] = new Matcher[X, String] {
		def apply(actual: Option[X], expected: String): Validation[Option[X]] = actual match {
			case Some(actualValue) => actual.success
			case None => "found nothing".failure
		}
		def name: String = "exists"
	}

	def notExists[X] = new Matcher[X, String] {
		def apply(actual: Option[X], expected: String): Validation[Option[X]] = actual match {
			case Some(actualValue) => s"unexpectedly found $actualValue".failure
			case None => None.success
		}
		def name: String = "notExists"
	}

	def whatever[X] = new Matcher[X, String] {
		def apply(actual: Option[X], expected: String): Validation[Option[X]] = actual.success
		def name: String = "whatever"
	}
}

trait Matcher[A, E] {
	def apply(actual: Option[A], expected: E): Validation[Option[A]]
	def name: String
}
