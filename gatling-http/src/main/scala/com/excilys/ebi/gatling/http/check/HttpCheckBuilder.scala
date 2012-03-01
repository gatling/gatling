/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
import com.excilys.ebi.gatling.core.check.{ ExtractorFactory, CheckBuilderFactory }
import com.excilys.ebi.gatling.core.check.{ CheckStrategy, CheckBaseBuilder }
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.http.request.HttpPhase.HttpPhase
import com.ning.http.client.Response

/**
 * This class serves as model for the HTTP-specific check builders
 *
 * @param expression the function returning the expression representing what is to be checked
 * @param to the optional session key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 * @param phase the HttpPhase during which the check will be made
 */
abstract class HttpCheckBuilder[X](val expression: EvaluatableString, val phase: HttpPhase) extends CheckBaseBuilder[HttpCheck[X], Response, X] {
	def httpCheckBuilderFactory[X]: CheckBuilderFactory[HttpCheck[X], Response, X] =
		(extractorFactory: ExtractorFactory[Response, X], strategy: CheckStrategy[X], saveAs: Option[String], transform: Option[X => Any]) =>
			new HttpCheck(expression, extractorFactory, strategy, saveAs, transform, phase)
}