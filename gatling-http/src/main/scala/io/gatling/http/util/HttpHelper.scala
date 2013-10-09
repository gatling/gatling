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

import scala.collection.JavaConversions.seqAsJavaList
import scala.io.Codec.UTF8

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, FluentStringsMap, ProxyServer, Realm }
import com.ning.http.client.Realm.AuthScheme

import io.gatling.core.config.Credentials
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import io.gatling.http.HeaderNames
import io.gatling.http.request.builder.HttpParam

object HttpHelper {

	val emptyParamListSuccess = List.empty[(String, Seq[String])].success

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

		params.foldLeft(emptyParamListSuccess) { (resolvedParams, param) =>
			val (key, values) = param
			for {
				resolvedParams <- resolvedParams
				key <- key(session)
				values <- values(session)
			} yield (key, values) :: resolvedParams
		}.map(_.reverse)
	}

	def httpParamsToFluentMap(params: List[HttpParam], session: Session): Validation[FluentStringsMap] =
		resolveParams(params, session).map { params =>

			params.groupBy(_._1).foldLeft(new FluentStringsMap) {
				case (fsm, (key, params)) =>
					val values = params.map(_._2).flatten
					fsm.add(key, values)
			}
		}

	def buildRealm(username: Expression[String], password: Expression[String]): Expression[Realm] = (session: Session) =>
		for {
			usernameValue <- username(session)
			passwordValue <- password(session)
		} yield buildRealm(usernameValue, passwordValue)

	def buildRealm(username: String, password: String): Realm = new Realm.RealmBuilder().setPrincipal(username).setPassword(password).setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC).build

	def buildProxy(host: String, port: Int, credentials: Option[Credentials], secure: Boolean) = {

		val protocol = if (secure) ProxyServer.Protocol.HTTPS else ProxyServer.Protocol.HTTP
		credentials
			.map(c => new ProxyServer(protocol, host, port, c.username, c.password))
			.getOrElse(new ProxyServer(protocol, host, port))
			.setNtlmDomain(null)
	}

	def isHtml(headers: FluentCaseInsensitiveStringsMap) = Option(headers.getFirstValue(HeaderNames.CONTENT_TYPE)).map(_.contains("html")).getOrElse(false)
}
