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
package com.excilys.ebi.gatling.http.check.status.extractor

import com.ning.http.client.Response
import com.excilys.ebi.gatling.core.check.extractor.Extractor

/**
 * HTTP Status extractor
 *
 * @constructor constructs an HttpHeaderExtractor
 * @param response the response in which the extraction will take place
 */
class HttpStatusExtractor(response: Response) extends Extractor {
	/**
	 * Extracts the status code from the response
	 *
	 * @param unused unused argument
	 * @return an Option containing the status code of the response
	 */
	def extract(unused: String): List[String] = List(response.getStatusCode.toString)
}