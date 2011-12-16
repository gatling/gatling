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
package com.excilys.ebi.gatling.http.check.header.extractor

import com.excilys.ebi.gatling.core.check.extractor.Extractor
import com.ning.http.client.{ Response, FluentCaseInsensitiveStringsMap }
import scala.collection.JavaConversions._

/**
 * HTTP Header extractor
 *
 * @constructor constructs an HttpHeaderExtractor
 * @param response the response in which the extraction will take place
 */
class HttpHeaderExtractor(response: Response) extends Extractor {

	/**
	 * Extracts the header requested
	 *
	 * @param headerName the name of the header
	 * @return an Option containing the value of the header
	 */
	def extract(headerName: String): List[String] = {

		response.getHeaders.get(headerName) match {
			case null => Nil
			case l => l.toList
		}
	}
}