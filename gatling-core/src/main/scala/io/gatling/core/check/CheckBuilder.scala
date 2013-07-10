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

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.session.{ Expression, noopStringExpression }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

trait ExtractorCheckBuilder[C <: Check[R], R, P, T, X] {

	def find: MatcherCheckBuilder[C, R, P, T, X]
}

trait MultipleExtractorCheckBuilder[C <: Check[R], R, P, T, X] extends ExtractorCheckBuilder[C, R, P, T, X] {

	def find(occurrence: Int): MatcherCheckBuilder[C, R, P, T, X]

	def findAll: MatcherCheckBuilder[C, R, P, T, X]

	def count: MatcherCheckBuilder[C, R, P, T, X]
}

case class MatcherCheckBuilder[C <: Check[R], R, P, T, X](
	checkFactory: CheckFactory[C, R],
	preparer: Preparer[R, P],
	extractor: Extractor[P, T, X],
	extractorCriterion: Expression[T]) extends Logging {

	def transform[X2](transformation: Option[X] => Option[X2]): MatcherCheckBuilder[C, R, P, T, X2] = copy(extractor = new Extractor[P, T, X2] {
		def name = extractor.name + " transformed"
		def apply(prepared: P, criterion: T): Validation[Option[X2]] = extractor(prepared, criterion).flatMap { extracted =>
			try {
				transformation(extracted).success
			} catch {
				case e: Exception => s"transform crashed with a exception: ${e.getMessage}".failure
			}
		}
	})

	def matchWith[E](matcher: Matcher[X, E], expected: Expression[E]) = new CheckBuilder(this, matcher, expected) with SaveAs[C, R, P, T, X, E]

	def is(expected: Expression[X]) = matchWith(Matchers.is, expected)
	def not(expected: Expression[X]) = matchWith(Matchers.not, expected)
	def in(expected: Expression[Seq[X]]) = matchWith(Matchers.in, expected)
	def exists = matchWith(Matchers.exists, noopStringExpression)
	def lessThan(expected: Expression[X]) = matchWith(Matchers.lessThan, expected)
	def notExists = matchWith(Matchers.notExists, noopStringExpression)
	def whatever = matchWith(Matchers.whatever, noopStringExpression)
}

case class CheckBuilder[C <: Check[R], R, P, T, X, E](
	matcherCheckBuilder: MatcherCheckBuilder[C, R, P, T, X],
	matcher: Matcher[X, E],
	expected: Expression[E],
	saveAs: Option[String] = None) {

	def build: C = {
		val base = CheckBase(matcherCheckBuilder.preparer, matcherCheckBuilder.extractor, matcherCheckBuilder.extractorCriterion, matcher, expected, saveAs)
		matcherCheckBuilder.checkFactory(base)
	}
}

trait SaveAs[C <: Check[R], R, P, T, X, E] { this: CheckBuilder[C, R, P, T, X, E] =>
	def saveAs(key: String): CheckBuilder[C, R, P, T, X, E] = copy(saveAs = Some(key))
}