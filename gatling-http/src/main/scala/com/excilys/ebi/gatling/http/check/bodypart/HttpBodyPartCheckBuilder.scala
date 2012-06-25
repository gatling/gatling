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
package com.excilys.ebi.gatling.http.check.bodypart

import com.excilys.ebi.gatling.core.check.{ MatcherCheckBuilder, ExtractorFactory }
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.ahc.ExtendedResponse
import com.excilys.ebi.gatling.http.check.{ HttpExtractorCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.check.bodypart.HttpBodyPartCheckBuilder.findExtractorFactory
import com.excilys.ebi.gatling.http.request.HttpPhase.BodyPartReceived
import com.ning.http.client.Response

/**
 * HttpBodyPartCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpBodyPartCheckBuilder {

	def checksum(algorythm: String) = new HttpBodyPartCheckBuilder(algorythm)

	private def findExtractorFactory: ExtractorFactory[Response, String, String] = (response: Response) =>
		(expression: String) => response.asInstanceOf[ExtendedResponse].checksum(expression)
}

/**
 * This class builds a body part check
 */
class HttpBodyPartCheckBuilder(algorythm: String) extends HttpExtractorCheckBuilder[String, String]((session: Session) => algorythm, BodyPartReceived) {

	def find = new MatcherCheckBuilder[HttpCheck[String], Response, String, String](httpCheckBuilderFactory, findExtractorFactory)
}