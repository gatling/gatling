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
package com.excilys.ebi.gatling.core.test

import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification

import scalaz.{ Failure, Success, Validation }

trait ScalazSpecification extends Specification {

	/** success matcher for a Validation */
	def beSuccessful[E, A]: Matcher[Validation[E, A]] = (v: Validation[E, A]) => (v.fold(_ => false, _ => true), v + " successful", v + " is not successfull")

	/** failure matcher for a Validation */
	def beAFailure[E, A]: Matcher[Validation[E, A]] = (v: Validation[E, A]) => (v.fold(_ => true, _ => false), v + " is a failure", v + " is not a failure")

	/** success matcher for a Validation with a specific value */
	def succeedWith[E, A](a: => A) = validationWith[E, A](Success(a))

	/** failure matcher for a Validation with a specific value */
	def failWith[E, A](e: => E) = validationWith[E, A](Failure(e))

	private def validationWith[E, A](f: => Validation[E, A]): Matcher[Validation[E, A]] = (v: Validation[E, A]) => {
		val expected = f
		(expected == v, v + " is a " + expected, v + " is not a " + expected)
	}
}