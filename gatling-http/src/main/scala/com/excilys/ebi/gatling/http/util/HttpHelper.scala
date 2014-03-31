/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.net.{ URLDecoder, URI }

import scala.collection.JavaConversions.{ asScalaBuffer, asScalaSet, seqAsJavaList }
import scala.io.Codec.UTF8

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE
import com.excilys.ebi.gatling.http.request.builder.HttpParam
import com.ning.http.client.FluentStringsMap

object HttpHelper {

	val httpScheme = "http"
	val httpsScheme = "https"
	val wsScheme = "ws"
	val wssScheme = "wss"

	def parseFormBody(body: String): List[(String, String)] = {
		def utf8Decode(s: String) = URLDecoder.decode(s, UTF8.name)

		body
			.split("&")
			.map(_.split("=", 2))
			.map { pair =>
				val paramName = utf8Decode(pair(0))
				val paramValue = if (pair.isDefinedAt(1)) utf8Decode(pair(1)) else ""
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

	def dumpFluentCaseInsensitiveStringsMap(map: java.util.Map[String, java.util.List[String]], buff: StringBuilder) {

		for {
			entry <- map.entrySet
			key = entry.getKey
			value <- entry.getValue
		} buff.append(key).append(": ").append(value).append(END_OF_LINE)
	}

	def isSecure(uri: URI) = uri.getScheme == httpsScheme || uri.getScheme == wssScheme
}