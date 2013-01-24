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

import com.excilys.ebi.gatling.core.check.{ Matcher, ExtractorCheckBuilder, CheckBuilderFactory }
import com.excilys.ebi.gatling.core.session.Expression
import com.excilys.ebi.gatling.http.request.HttpPhase.HttpPhase
import com.excilys.ebi.gatling.http.response.ExtendedResponse

/**
 * This class serves as model for the HTTP-specific check builders
 *
 * @param expression the function returning the expression representing what is to be checked
 * @param phase the HttpPhase during which the check will be made
 */
abstract class HttpExtractorCheckBuilder[X, XC](expression: Expression[XC], phase: HttpPhase) extends ExtractorCheckBuilder[HttpCheck[XC], ExtendedResponse, XC, X] {

	val httpCheckBuilderFactory: CheckBuilderFactory[HttpCheck[XC], ExtendedResponse, XC] = (matcher: Matcher[ExtendedResponse, XC], saveAs: Option[String]) => new HttpCheck(expression, matcher, saveAs, phase)
}