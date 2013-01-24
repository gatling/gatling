/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.collection.JavaConversions.{ asScalaBuffer, asScalaSet, seqAsJavaList }
import scala.io.Codec.UTF8

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.FlattenableValidations
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE
import com.excilys.ebi.gatling.http.request.builder.HttpParam
import com.ning.http.client.FluentStringsMap

import scalaz.Validation

object HttpHelper {

	def computeRedirectUrl(locationHeader: String, originalRequestUrl: String) = {
		if (locationHeader.startsWith("http")) // as of the RFC, Location should be an absolute uri
			locationHeader
		else {
			// sadly, internet is a mess
			val (locationPathPart, locationQueryPart) = locationHeader.indexOf('?') match {
				case -1 => (locationHeader, null)
				case queryMarkIndex => (locationHeader.substring(0, queryMarkIndex), locationHeader.substring(queryMarkIndex + 1))
			}

			val originalRequestURI = new URI(originalRequestUrl)
			val originalRequestPath = originalRequestURI.getPath

			val absolutePath = if (locationPathPart.startsWith("/"))
				locationPathPart
			else {
				originalRequestPath.lastIndexOf('/') match {
					case -1 => "/" + locationPathPart
					case lastSlashIndex => originalRequestPath.substring(0, lastSlashIndex + 1) + locationPathPart
				}
			}

			new URI(originalRequestURI.getScheme, null, originalRequestURI.getHost, originalRequestURI.getPort, absolutePath, locationQueryPart, null).toString
		}
	}

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

	def httpParamsToFluentMap(params: List[HttpParam], session: Session): Validation[String, FluentStringsMap] = {

		def httpParamsToFluentMap(params: List[(String, Seq[String])]): FluentStringsMap =
			params.groupBy(_._1)
				.mapValues(_.map(_._2).flatten)
				.foldLeft(new FluentStringsMap) { (map, keyValues) =>
					val (key, values) = keyValues
					map.add(key, values)
				}

		val validations = params
			.map {
				case (key, values) =>
					for {
						resolvedKey <- key(session)
						resolvedValues <- values(session)
					} yield (resolvedKey, resolvedValues)
			}

		validations.flattenIt.map(httpParamsToFluentMap)
	}

	def dumpFluentCaseInsensitiveStringsMap(map: java.util.Map[String, java.util.List[String]], buff: StringBuilder) {

		for {
			entry <- map.entrySet
			key = entry.getKey
			value <- entry.getValue
		} buff.append(key).append(": ").append(value).append(END_OF_LINE)
	}
}