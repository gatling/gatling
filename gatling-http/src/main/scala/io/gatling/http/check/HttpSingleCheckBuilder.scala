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
package com.excilys.ebi.gatling.http.check

import com.excilys.ebi.gatling.core.check.{ CheckFactory, Extractor, ExtractorCheckBuilder, MatcherCheckBuilder, Preparer }
import com.excilys.ebi.gatling.core.session.Expression
import com.excilys.ebi.gatling.http.response.ExtendedResponse

class HttpSingleCheckBuilder[P, T, X](
	checkFactory: CheckFactory[HttpCheck, ExtendedResponse],
	preparer: Preparer[ExtendedResponse, P],
	extractor: Extractor[P, T, X],
	expression: Expression[T]) extends ExtractorCheckBuilder[HttpCheck, ExtendedResponse, P, T, X] {

	def find: MatcherCheckBuilder[HttpCheck, ExtendedResponse, P, T, X] = MatcherCheckBuilder(checkFactory, preparer, extractor, expression)
}