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

import com.excilys.ebi.gatling.core.check.Check
import com.excilys.ebi.gatling.core.validation.Validation

package object check {

	type Preparer[R, P] = R => Validation[P]

	trait Extractor[P, T, X] {
		def name: String
		def apply(prepared: P, criterion: T): Validation[Option[X]]
	}

	object Matchers {

		def is[X] = new Matcher[X, X] {
			def apply(actual: Option[X], expected: X): Boolean = actual.map(_ == expected).getOrElse(false)
			def name: String = "is"
		}

		def not[X] = new Matcher[X, X] {
			def apply(actual: Option[X], expected: X): Boolean = actual.map(_ != expected).getOrElse(true)
			def name: String = "not"
		}

		def in[X] = new Matcher[X, Seq[X]] {
			def apply(actual: Option[X], expected: Seq[X]): Boolean = actual.map(expected.contains).getOrElse(false)
			def name: String = "in"
		}

		// TODO extend so that any kind of numbers can be compared in any way
		def lessThan[X] = new Matcher[X, X] {
			def apply(actual: Option[X], expected: X): Boolean = (actual, expected) match {
				case (Some(a: Long), e: Long) => a <= e
				case (Some(a: Int), e: Int) => a <= e
				case (Some(a: Double), e: Double) => a <= e
				case (Some(a: Float), e: Float) => a <= e
				case _ => false
			}
			def name: String = "lessThan"
		}

		val exists = new Matcher[Any, String] {
			def apply(actual: Option[Any], expected: String): Boolean = actual.isDefined
			def name: String = "exists"
		}

		val notExists = new Matcher[Any, Any] {
			def apply(actual: Option[Any], expected: Any): Boolean = !actual.isDefined
			def name: String = "notExists"
		}

		val whatever = new Matcher[Any, Any] {
			def apply(actual: Option[Any], expected: Any): Boolean = true
			def name: String = "whatever"
		}
	}

	trait Matcher[-A, E] {
		def apply(actual: Option[A], expected: E): Boolean
		def name: String
	}

	type CheckFactory[C <: Check[R], R] = Check[R] => C
}
