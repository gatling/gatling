/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder.ahc

import java.net.URI

import com.ning.http.client.RequestBuilder

import io.gatling.core.session.Session
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.cache.CacheHandling
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.{ CommonAttributes, HttpAttributes }

class AHCHttpRequestBuilder(
	commonAttributes: CommonAttributes,
	httpAttributes: HttpAttributes,
	session: Session,
	protocol: HttpProtocol) extends AHCRequestBuilder(commonAttributes, session, protocol) {

	def makeAbsolute(url: String): Validation[String] =
		if (url.startsWith("http"))
			url.success
		else
			protocol.baseURL match {
				case Some(baseURL) => (baseURL + url).success
				case _ => s"No protocol.baseURL defined but provided url is relative : $url".failure
			}

	def configureCaches(uri: URI)(requestBuilder: RequestBuilder): Validation[RequestBuilder] = {
		CacheHandling.getLastModified(protocol, session, uri).foreach(requestBuilder.setHeader(HeaderNames.IF_MODIFIED_SINCE, _))
		CacheHandling.getEtag(protocol, session, uri).foreach(requestBuilder.setHeader(HeaderNames.IF_NONE_MATCH, _))
		requestBuilder.success
	}

	def configureParts(requestBuilder: RequestBuilder): Validation[RequestBuilder] = {
		require(!httpAttributes.body.isDefined || httpAttributes.bodyParts.isEmpty, "Can't have both a body and body parts!")

		httpAttributes.body match {
			case Some(body) =>
				body.setBody(requestBuilder, session)

			case None =>
				httpAttributes.bodyParts match {
					case Nil => requestBuilder.success
					case bodyParts =>
						if (!commonAttributes.headers.contains(HeaderNames.CONTENT_TYPE))
							requestBuilder.addHeader(HeaderNames.CONTENT_TYPE, HeaderValues.MULTIPART_FORM_DATA)

						bodyParts.foldLeft(requestBuilder.success) { (requestBuilder, part) =>
							for {
								requestBuilder <- requestBuilder
								part <- part.toMultiPart(session)
							} yield requestBuilder.addBodyPart(part)
						}
				}
		}
	}

	override protected def configureRequestBuilder(uri: URI, requestBuilder: RequestBuilder): Validation[RequestBuilder] =
		super.configureRequestBuilder(uri, requestBuilder)
			.flatMap(configureCaches(uri))
			.flatMap(configureParts)
}
