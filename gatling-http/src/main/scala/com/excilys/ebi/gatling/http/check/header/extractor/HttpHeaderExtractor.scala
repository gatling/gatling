/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
	def extract(headerName: String): Option[String] = {
		extractAll(headerName, response.getHeaders()).map { list =>
			if (list.size > 0)
				Some(list.get(0))
			else
				None
		}.getOrElse(None)
	}

	/**
	 * Extracts a header from a FluentCaseInsensitiveStringsMap of headers
	 *
	 * @param headerName the name of the header
	 * @param headersMap the map containing all the headers received
	 * @return an Option containing the value of the header
	 */
	def extractAll(headerName: String, headersMap: FluentCaseInsensitiveStringsMap): Option[java.util.List[String]] = {

		val values = headersMap.get(headerName)

		if (values == null)
			None
		else
			Some(values)
	}
}