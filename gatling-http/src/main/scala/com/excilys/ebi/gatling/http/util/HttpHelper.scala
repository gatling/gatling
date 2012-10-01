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

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.seqAsJavaList
import scala.io.Codec.UTF8

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.request.builder.HttpParam
import com.ning.http.client.FluentStringsMap

object HttpHelper {

	def computeRedirectUrl(locationHeader: String, originalRequestUrl: String) = {
		if (locationHeader.startsWith("http")) // as of the RFC, Location should be an absolute uri
			locationHeader
		else { // sadly, internet is a mess
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
		def utf8Decode(s: String) = URLDecoder.decode(s, UTF8.name)

		body
			.split("&")
			.map(_.split("=", 2))
			.map { pair =>
				val paramName = utf8Decode(pair(0))
				val paramValue = if (pair.isDefinedAt(1)) utf8Decode(pair(1)) else EMPTY
				paramName -> paramValue
			}.toList
	}

	def httpParamsToFluentMap(params: List[HttpParam], session: Session): FluentStringsMap = params
		.map { case (key, value) => (key(session), value(session)) }
		.groupBy(_._1)
		.mapValues(_.map(_._2).flatten)
		.foldLeft(new FluentStringsMap) { (map, keyValues) =>
			val (key, values) = keyValues
			map.add(key, values)
		}
}