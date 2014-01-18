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
package io.gatling.recorder.scenario

import java.nio.charset.Charset

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.handler.codec.http.{ HttpRequest, HttpResponse }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.{ AUTHORIZATION, CONTENT_TYPE }
import org.jboss.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED

import com.ning.http.util.Base64

import io.gatling.http.fetch.{ EmbeddedResource, HtmlParser }
import io.gatling.http.util.HttpHelper.parseFormBody
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.scenario.template.RequestTemplate
import io.gatling.recorder.util.URIHelper

sealed trait RequestBody
case class RequestBodyParams(params: List[(String, String)]) extends RequestBody
case class RequestBodyBytes(bytes: Array[Byte]) extends RequestBody

object RequestElement {

	def apply(request: HttpRequest, response: HttpResponse): RequestElement = {

		val htmlContentType = """text/html\s*(;\s+charset=(.+))?$""".r

		val responseContentType = Option(response.headers.get(CONTENT_TYPE))
		val resources = responseContentType.collect {
			case htmlContentType(_, headerCharset) => {
				val charsetName = Option(headerCharset).filter(Charset.isSupported).getOrElse("UTF-8")
				val charset = Charset.forName(charsetName)
				val htmlContent = response.getContent.toString(charset)

				HtmlParser.getEmbeddedResources(new java.net.URI(request.getUri), htmlContent.toCharArray)
			}
		}.getOrElse(Nil)

		val requestHeaders: Map[String, String] = request.headers.entries.map { entry => (entry.getKey, entry.getValue) }.toMap
		val content = if (request.getContent.readableBytes > 0) {
			val bufferBytes = new Array[Byte](request.getContent.readableBytes)
			request.getContent.getBytes(request.getContent.readerIndex, bufferBytes)
			Some(bufferBytes)
		} else None

		val containsFormParams = requestHeaders.get(CONTENT_TYPE).exists(_.contains(APPLICATION_X_WWW_FORM_URLENCODED))
		val body = content.map(content =>
			if (containsFormParams) RequestBodyParams(parseFormBody(new String(content, configuration.core.encoding)))
			else RequestBodyBytes(content))

		RequestElement(new String(request.getUri), request.getMethod.toString, requestHeaders, body, response.getStatus.getCode, resources)
	}
}

case class RequestElement(uri: String, method: String, headers: Map[String, String], body: Option[RequestBody],
	statusCode: Int, embeddedResources: List[EmbeddedResource]) extends ScenarioElement {

	val (baseUrl, pathQuery) = {
		val (rawBaseUrl, pathQuery) = URIHelper.splitURI(uri)

		val baseUrl = if (rawBaseUrl.startsWith("https://"))
			rawBaseUrl.stripSuffix(":443")
		else
			rawBaseUrl.stripSuffix(":80")

		(baseUrl, pathQuery)
	}
	private var printedUrl = uri

	var filteredHeadersId: Option[Int] = None

	var id: Int = 0

	def setId(id: Int) = {
		this.id = id
		this
	}

	def makeRelativeTo(baseUrl: String): RequestElement = {
		if (baseUrl == this.baseUrl)
			printedUrl = pathQuery
		this
	}

	private val basicAuthCredentials: Option[(String, String)] = {
		def parseCredentials(header: String) =
			new String(Base64.decode(header.split(" ")(1))).split(":") match {
				case Array(username, password) => Some(username, password)
				case _ => None
			}

		headers.get(AUTHORIZATION).filter(_.startsWith("Basic ")).flatMap(parseCredentials)
	}

	override def toString =
		RequestTemplate.render(
			configuration.core.className,
			id,
			method,
			printedUrl,
			filteredHeadersId,
			basicAuthCredentials,
			body,
			statusCode)
}
