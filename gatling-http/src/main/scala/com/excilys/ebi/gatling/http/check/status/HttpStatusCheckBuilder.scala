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
package com.excilys.ebi.gatling.http.check.status

import com.excilys.ebi.gatling.core.check.extractor.ExtractorFactory
import com.excilys.ebi.gatling.core.check.extractor.Extractor
import com.excilys.ebi.gatling.core.check.CheckOneBuilder
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.StatusReceived
import com.ning.http.client.Response

/**
 * HttpStatusCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpStatusCheckBuilder {

	def status = new HttpStatusCheckBuilder
}

/**
 * This class builds a response status check
 *
 * @param to the optional session key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpStatusCheckBuilder extends HttpCheckBuilder[Int](Session => EMPTY, StatusReceived) {

	def find = new CheckOneBuilder[HttpCheck[Int], Response, Int](checkBuildFunction[Int], new ExtractorFactory[Response, Int] {
		def getExtractor(response: Response) = new Extractor[Int] {
			def extract(expression: String) = Some(response.getStatusCode)
		}
	})
}
