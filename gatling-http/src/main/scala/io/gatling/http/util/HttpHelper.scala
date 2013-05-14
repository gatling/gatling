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
package io.gatling.http.util

import java.net.URLDecoder
import java.util.{ ArrayList => JArrayList }

import scala.collection.JavaConversions.mapAsJavaMap
import scala.io.Codec.UTF8

import com.ning.http.client.{ FluentStringsMap, Realm }
import com.ning.http.client.Realm.AuthScheme

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ Validation, ValidationList }
import io.gatling.http.request.builder.HttpParam

object HttpHelper {

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
		val validations = params.map {
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

	def buildRealm(username: Expression[String], password: Expression[String]): Expression[Realm] = (session: Session) =>
		for {
			usernameValue <- username(session)
			passwordValue <- password(session)
		} yield new Realm.RealmBuilder().setPrincipal(usernameValue).setPassword(passwordValue).setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC).build
}