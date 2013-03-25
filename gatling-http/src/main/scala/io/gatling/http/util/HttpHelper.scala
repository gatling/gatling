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
package io.gatling.http.util

import java.net.{ URI, URLDecoder }
import java.util.{ ArrayList => JArrayList }

import scala.collection.JavaConversions.{ asScalaSet, mapAsJavaMap }
import scala.io.Codec.UTF8

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.StringHelper.eol
import io.gatling.core.validation.{ Validation, ValidationList }
import io.gatling.http.request.builder.HttpParam
import com.ning.http.client.{ FluentStringsMap, Realm }
import com.ning.http.client.Realm.AuthScheme

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

	def resolveParams(params: List[HttpParam], session: Session): Validation[List[(String, Seq[String])]] = {
		val validations = params
			.map {
				case (key, values) =>
					for {
						resolvedKey <- key(session)
						resolvedValues <- values(session)
					} yield (resolvedKey, resolvedValues)
			}

		validations.sequence
	}

	def httpParamsToFluentMap(params: List[HttpParam], session: Session): Validation[FluentStringsMap] = {

		resolveParams(params, session).map { params =>
			val javaParams = params.groupBy(_._1)
				.mapValues { params =>
					val arrayList = new JArrayList[String]
					for {
						param <- params
						value <- param._2
					} arrayList.add(value)
					arrayList
				}

			new FluentStringsMap(javaParams)
		}
	}

	def dumpFluentCaseInsensitiveStringsMap(map: java.util.Map[String, java.util.List[String]], buff: StringBuilder) {

		for {
			entry <- map.entrySet
		} buff.append(entry.getKey).append(": ").append(entry.getValue).append(eol)
	}

	def buildRealm(username: Expression[String], password: Expression[String]): Expression[Realm] = (session: Session) =>
		for {
			usernameValue <- username(session)
			passwordValue <- password(session)
		} yield new Realm.RealmBuilder().setPrincipal(usernameValue).setPassword(passwordValue).setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC).build
}