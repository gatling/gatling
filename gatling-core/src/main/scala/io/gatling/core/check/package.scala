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
package io.gatling.core

import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

package object check {

	type Preparer[R, P] = R => Validation[P]

	trait Extractor[P, T, X] {
		def name: String
		def apply(prepared: P, criterion: T): Validation[Option[X]]
	}

	object Matchers {

		def is[X] = new Matcher[X, X] {
			def apply(actual: Option[X], expected: X): Validation[Option[X]] = actual match {
				case Some(actualValue) =>
					if (actualValue == expected)
						actual.success
					else
						s"expected $expected but found $actualValue".failure
				case None => s"expected $expected but found nothing".failure
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
						s"expected $expected but found $actualValue".failure
				case None => s"expected $expected but found nothing".failure
			}
			def name: String = "in"
		}

		type Comparison[X] = (X, X) => Boolean

		class Comparer[X](
			val name: String,
			message: String,
			byteComparison: Comparison[Byte],
			shortComparison: Comparison[Short],
			charComparison: Comparison[Char],
			intComparison: Comparison[Int],
			longComparison: Comparison[Long],
			floatComparison: Comparison[Float],
			doubleComparison: Comparison[Double],
			comparableComparison: Int => Boolean) extends Matcher[X, X] {

			def apply(actual: Option[X], expected: X): Validation[Option[X]] =
				actual.map { actualValue =>
					val comparison = (actualValue, expected) match {
						case (a: Byte, e: Byte) => byteComparison(a, e)
						case (a: Short, e: Short) => shortComparison(a, e)
						case (a: Char, e: Char) => charComparison(a, e)
						case (a: Int, e: Int) => intComparison(a, e)
						case (a: Long, e: Long) => longComparison(a, e)
						case (a: Float, e: Float) => floatComparison(a, e)
						case (a: Double, e: Double) => doubleComparison(a, e)
						case (a: Comparable[X], e) => comparableComparison(a.compareTo(e))
						case _ => false
					}

					if (comparison) actual.success else s"$actualValue is not $message than $expected".failure

				}.getOrElse(s"can't compare nothing and $expected".failure)
		}

		def lessThan[X] = new Comparer[X]("lessThan", "less", _ < _, _ < _, _ < _, _ < _, _ < _, _ < _, _ < _, _ < 0)
		def lessOrEqualThan[X] = new Comparer[X]("lessOrEqualThan", "less or equal", _ <= _, _ <= _, _ <= _, _ <= _, _ <= _, _ <= _, _ <= _, _ <= 0)
		def greaterThan[X] = new Comparer[X]("greaterThan", "greater", _ > _, _ > _, _ > _, _ > _, _ > _, _ > _, _ > _, _ > 0)
		def greaterOrEqualThan[X] = new Comparer[X]("greaterOrEqualThan", "greater or equal", _ >= _, _ >= _, _ >= _, _ >= _, _ >= _, _ >= _, _ >= _, _ >= 0)

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

	type CheckFactory[C <: Check[R], R] = Check[R] => C
}
