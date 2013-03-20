/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core

import com.excilys.ebi.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

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

		// TODO extend so that any kind of numbers can be compared in any way
		def lessThan[X] = new Matcher[X, X] {
			def apply(actual: Option[X], expected: X): Validation[Option[X]] = (actual, expected) match {
				case (Some(a: Long), e: Long) => if (a <= e) actual.success else s"$a > $e".failure
				case (Some(a: Int), e: Int) => if (a <= e) actual.success else s"$a > $e".failure
				case (Some(a: Double), e: Double) => if (a <= e) actual.success else s"$a > $e".failure
				case (Some(a: Float), e: Float) => if (a <= e) actual.success else s"$a > $e".failure
				case _ => s"can't compare ${actual.getOrElse("nothing")} and $expected".failure
			}
			def name: String = "lessThan"
		}

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
