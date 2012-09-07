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

import java.net.{ URI, URLDecoder }

import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

object HttpHelper {

	def computeRedirectUrl(locationHeader: String, originalRequestUrl: String) = {
		if (locationHeader.startsWith("http"))
			locationHeader
		else {
			val originalRequestURI = new URI(originalRequestUrl)
			val originalRequestPath = originalRequestURI.getPath
			val newPath = if (locationHeader.charAt(0) == '/')
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

	def parseFormBody(body: String): List[(String, String)] = {
		def utf8Decode(s: String) = URLDecoder.decode(s, "UTF-8")

		body
			.split("&")
			.map(_.split("="))
			.map { pair =>

				val paramName = utf8Decode(pair(0))
				val paramValue = if (pair.isDefinedAt(1)) utf8Decode(pair(1)) else EMPTY

				paramName -> paramValue
			}.toList
	}
}