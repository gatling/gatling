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
package io.gatling.http.fetch

import java.net.URI

import com.ning.http.client.Request

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ Success, SuccessWrapper }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.HttpRequestBaseBuilder

case class NamedRequest(name: String, ahcRequest: Request)

sealed abstract class EmbeddedResource {

	def uri: URI
	def url: String

	def toRequest(protocol: HttpProtocol): Option[NamedRequest] = {
		val urlExpression: Expression[String] = _ => url.success
		val httpRequest = HttpRequestBaseBuilder.http(urlExpression).get(uri).build(protocol, false)
		
		// TODO NICOLAS - fix that weird hardcoded session ?
		httpRequest.ahcRequest(Session("foo", "bar")) match {
			case Success(ahcRequest) => {

				val requestName = {
					val start = url.lastIndexOf('/') + 1
					if (start < url.length)
						url.substring(start, url.length)
					else
						"/"
				}

				Some(NamedRequest(requestName, ahcRequest))
			}

			case _ => None
		}
	}
}

case class CssResource(uri: URI) extends EmbeddedResource {
	val url = uri.toString
}

case class RegularResource(uri: URI) extends EmbeddedResource {
	val url = uri.toString
}

