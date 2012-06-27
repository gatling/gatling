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

import scala.annotation.implicitNotFound

import com.excilys.ebi.gatling.core.check.{ MatcherCheckBuilder, Matcher, ExtractorFactory, CheckBuilderFactory }
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.check.{ HttpExtractorCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.check.bodypart.HttpChecksumCheckBuilder.findExtractorFactory
import com.excilys.ebi.gatling.http.request.HttpPhase.BodyPartReceived
import com.excilys.ebi.gatling.http.response.ExtendedResponse

/**
 * HttpChecksumCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpChecksumCheckBuilder {

	def checksum(algorythm: String) = new HttpChecksumCheckBuilder(algorythm)

	private val findExtractorFactory: ExtractorFactory[ExtendedResponse, String, String] = (response: ExtendedResponse) =>
		(expression: String) => response.checksum(expression)
}

/**
 * This class builds a checksum check
 */
class HttpChecksumCheckBuilder(algorythm: String) extends HttpExtractorCheckBuilder[String, String]((session: Session) => algorythm, BodyPartReceived) {

	val checksumCheckBuilderFactory: CheckBuilderFactory[HttpCheck[String], ExtendedResponse, String] = (matcher: Matcher[ExtendedResponse, String], saveAs: Option[String]) => new ChecksumCheck(algorythm, matcher, saveAs)

	def find = new MatcherCheckBuilder[HttpCheck[String], ExtendedResponse, String, String](checksumCheckBuilderFactory, findExtractorFactory)
}