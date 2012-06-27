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

import com.excilys.ebi.gatling.core.check.{ Matcher, ExtractorFactory, CheckBuilderFactory }
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.{ HttpSingleCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.BodyPartReceived
import com.excilys.ebi.gatling.http.response.ExtendedResponse

/**
 * HttpChecksumCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpChecksumCheckBuilder {

	def checksum(algorythm: String) = {

		val checksumCheckBuilderFactory = (matcher: Matcher[ExtendedResponse, String], saveAs: Option[String]) => new ChecksumCheck(algorythm, matcher, saveAs)
		val findExtractorFactory = (response: ExtendedResponse) => (unused: String) => response.checksum(algorythm)

		new HttpChecksumCheckBuilder(checksumCheckBuilderFactory, findExtractorFactory)
	}
}

/**
 * This class builds a checksum check
 */
class HttpChecksumCheckBuilder(
	override val httpCheckBuilderFactory: CheckBuilderFactory[HttpCheck[String], ExtendedResponse, String],
	findExtractorFactory: ExtractorFactory[ExtendedResponse, String, String]) extends HttpSingleCheckBuilder[String, String](findExtractorFactory, (session: Session) => EMPTY, BodyPartReceived)