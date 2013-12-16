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

import java.net.{ URI, URLDecoder }

import scala.collection.breakOut
import scala.collection.JavaConversions.seqAsJavaList
import scala.io.Codec.UTF8

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, FluentStringsMap, ProxyServer, Realm }
import com.ning.http.client.Realm.AuthScheme
import com.ning.http.util.AsyncHttpProviderUtils
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.config.Credentials
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.request.builder.{ HttpParam, MultivaluedParam, ParamMap, ParamSeq, SimpleParam }

object HttpHelper extends StrictLogging {

	val emptyParamListSuccess = List.empty[(String, String)].success

	def parseFormBody(body: String): List[(String, String)] = {
		def utf8Decode(s: String) = URLDecoder.decode(s, UTF8.name)

		body
			.split("&")
			.map(_.split("=", 2))
			.map { pair =>
				val paramName = utf8Decode(pair(0))
				val paramValue = if (pair.isDefinedAt(1)) utf8Decode(pair(1)) else ""
				paramName -> paramValue
			}(breakOut)
	}

	def resolveParams(params: List[HttpParam], session: Session): Validation[Map[String, Seq[String]]] = {

		val resolvedParams = params.foldLeft(emptyParamListSuccess) { (resolvedParams, param) =>
			{

				val newParams: Validation[List[(String, String)]] = param match {
					case SimpleParam(key, value) =>
						for {
							key <- key(session)
							value <- value(session)
						} yield List(key -> value.toString)

					case MultivaluedParam(key, values) =>
						for {
							key <- key(session)
							values <- values(session)
						} yield values.map(key -> _.toString)(breakOut)

					case ParamSeq(seq) =>
						for {
							seq <- seq(session)
						} yield seq.map { case (key, value) => key -> value.toString }(breakOut)

					case ParamMap(map) =>
						for {
							map <- map(session)
						} yield map.map { case (key, value) => key -> value.toString }(breakOut)
				}

				for {
					newParams <- newParams
					resolvedParams <- resolvedParams
				} yield newParams ::: resolvedParams
			}
		}

		// group by name
		resolvedParams.map(_.groupBy(_._1).mapValues(_.map(_._2)))
	}

	def httpParamsToFluentMap(params: List[HttpParam], session: Session): Validation[FluentStringsMap] =
		resolveParams(params, session).map { params =>

			params.foldLeft(new FluentStringsMap) {
				case (fsm, (key, values)) => fsm.add(key, values)
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
		val proxyServer = credentials match {
			case Some(c) => new ProxyServer(protocol, host, port, c.username, c.password)
			case _ => new ProxyServer(protocol, host, port)
		}
		proxyServer.setNtlmDomain(null)
	}

	def isCss(headers: FluentCaseInsensitiveStringsMap) = Option(headers.getFirstValue(HeaderNames.CONTENT_TYPE)).exists(_.contains(HeaderValues.TEXT_CSS))
	def isHtml(headers: FluentCaseInsensitiveStringsMap) = Option(headers.getFirstValue(HeaderNames.CONTENT_TYPE)).exists(ct => ct.contains(HeaderValues.TEXT_HTML) || ct.contains(HeaderValues.APPLICATION_XHTML))
	def resolveFromURI(rootURI: URI, relative: String) = AsyncHttpProviderUtils.getRedirectUri(rootURI, relative)
	def resolveFromURISilently(rootURI: URI, relative: String): Option[URI] =
		try {
			Some(resolveFromURI(rootURI, relative))
		} catch {
			case e: Exception =>
				logger.error("Failed to resolve URI", e)
				None
		}
}
