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
package com.excilys.ebi.gatling.http.util
import java.net.URI

import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE
import com.ning.http.client.Response

object HttpHelper {

	implicit def toRichResponse(response: Response) = new RichResponse(response)

	def computeRedirectUrl(locationHeader: String, originalRequestUrl: String) = {
		if (locationHeader.startsWith("http"))
			locationHeader
		else {
			val originalRequestURI = new URI(originalRequestUrl)
			val originalRequestPath = originalRequestURI.getPath
			val newPath = if (locationHeader.startsWith("/"))
				locationHeader
			else {
				val index = originalRequestPath.lastIndexOf('/')
				if (index == -1)
					"/" + locationHeader
				else
					originalRequestPath.substring(0, index + 1) + locationHeader
			}
			new URI(originalRequestURI.getScheme, null, originalRequestURI.getHost, originalRequestURI.getPort, newPath, null, null).toString
		}
	}
}

class RichResponse(response: Response) {

	def dump: StringBuilder = {
		val buff = new StringBuilder().append(END_OF_LINE)
		if (response.hasResponseStatus)
			buff.append("status=").append(END_OF_LINE).append(response.getStatusCode()).append(" ").append(response.getStatusText()).append(END_OF_LINE)

		if (response.hasResponseHeaders)
			buff.append("headers= ").append(END_OF_LINE).append(response.getHeaders()).append(END_OF_LINE)

		if (response.hasResponseBody)
			buff.append("body=").append(END_OF_LINE).append(response.getResponseBody())

		buff
	}
}