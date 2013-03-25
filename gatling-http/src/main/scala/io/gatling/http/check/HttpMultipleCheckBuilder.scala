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
package io.gatling.http.check

import io.gatling.core.check.{ CheckFactory, Extractor, ExtractorCheckBuilder, MatcherCheckBuilder, Preparer }
import io.gatling.core.session.Expression
import io.gatling.http.response.ExtendedResponse

class HttpMultipleCheckBuilder[P, T, X](
	checkFactory: CheckFactory[HttpCheck, ExtendedResponse],
	preparer: Preparer[ExtendedResponse, P],
	findExtractor: Int => Extractor[P, T, X],
	findAllExtractor: Extractor[P, T, Seq[X]],
	countExtractor: Extractor[P, T, Int],
	expression: Expression[T]) extends ExtractorCheckBuilder[HttpCheck, ExtendedResponse, P, T, X] {

	def find = find(0)
	def find(occurrence: Int): MatcherCheckBuilder[HttpCheck, ExtendedResponse, P, T, X] = MatcherCheckBuilder(checkFactory, preparer, findExtractor(occurrence), expression)
	def findAll: MatcherCheckBuilder[HttpCheck, ExtendedResponse, P, T, Seq[X]] = MatcherCheckBuilder(checkFactory, preparer, findAllExtractor, expression)
	def count: MatcherCheckBuilder[HttpCheck, ExtendedResponse, P, T, Int] = MatcherCheckBuilder(checkFactory, preparer, countExtractor, expression)
}