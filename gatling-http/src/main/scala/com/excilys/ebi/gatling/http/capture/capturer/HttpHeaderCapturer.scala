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
package com.excilys.ebi.gatling.http.capture.capturer

import com.ning.http.client.{Response, FluentCaseInsensitiveStringsMap}
import com.excilys.ebi.gatling.core.capture.capturer.Capturer

class HttpHeaderCapturer(response: Response) extends Capturer {

	def capture(headerName: Any): Option[String] = {
		captureAll(headerName.toString, response.getHeaders()).map { list =>
			if (list.size > 0)
				Some(list.get(0))
			else
				None
		}.getOrElse(None)
	}

	def captureAll(headerName: String, headersMap: FluentCaseInsensitiveStringsMap): Option[java.util.List[String]] = {

		val values = headersMap.get(headerName)

		logger.debug(" -- Headers Capture Provider - Got header values: {}", values)

		if (values == null)
			None
		else
			Some(values)
	}
}