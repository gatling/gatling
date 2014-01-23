/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.net.URI
import java.nio.charset.Charset

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.duration.FiniteDuration
import scala.io.Codec.UTF8

import org.jboss.netty.handler.codec.http.{ HttpRequest, HttpResponse }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.{ AUTHORIZATION, CONTENT_TYPE }
import org.jboss.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED

import com.ning.http.util.Base64

import io.gatling.http.fetch.{ EmbeddedResource, HtmlParser }
import io.gatling.http.util.HttpHelper.parseFormBody
import io.gatling.recorder.util.URIHelper

sealed trait ScenarioElement

case class PauseElement(duration: FiniteDuration) extends ScenarioElement
case class TagElement(text: String) extends ScenarioElement

sealed trait RequestBody
case class RequestBodyParams(params: List[(String, String)]) extends RequestBody
case class RequestBodyBytes(bytes: Array[Byte]) extends RequestBody

object RequestElement {

	val htmlContentType = """(?i)text/html\s*(;\s+charset=(.+))?""".r

	def apply(request: HttpRequest, response: HttpResponse): RequestElement = {
		val requestHeaders: Map[String, String] = request.headers.entries.map { entry => (entry.getKey, entry.getValue) }.toMap
		val responseContentType = requestHeaders.get(CONTENT_TYPE)

		val contentBuff = if (request.getContent.readableBytes > 0) {
			val bufferBytes = new Array[Byte](request.getContent.readableBytes)
			request.getContent.getBytes(request.getContent.readerIndex, bufferBytes)
			Some(bufferBytes)
		} else None

		val resources = responseContentType.collect {
			case htmlContentType(_, headerCharset) => {
				val charsetName = Option(headerCharset).filter(Charset.isSupported).getOrElse(UTF8.name)
				val charset = Charset.forName(charsetName)
				contentBuff.map(bytes => {
					val htmlBuff = new String(bytes, charset).toCharArray
					HtmlParser.getEmbeddedResources(new URI(request.getUri), htmlBuff)
				})
			}
		}.flatten.getOrElse(Nil)

		val containsFormParams = responseContentType.exists(_.contains(APPLICATION_X_WWW_FORM_URLENCODED))
		val body = contentBuff.map(content =>
			if (containsFormParams)
				// The payload consists of a Unicode string using only characters in the range U+0000 to U+007F
				// cf: http://www.w3.org/TR/html5/forms.html#application/x-www-form-urlencoded-decoding-algorithm
				RequestBodyParams(parseFormBody(new String(content, UTF8.name)))
			else
				RequestBodyBytes(content))

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
	var printedUrl = uri

	// TODO NICO mutable external fields are a very bad idea
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

	val basicAuthCredentials: Option[(String, String)] = {
		def parseCredentials(header: String) =
			new String(Base64.decode(header.split(" ")(1))).split(":") match {
				case Array(username, password) => Some(username, password)
				case _ => None
			}

		headers.get(AUTHORIZATION).filter(_.startsWith("Basic ")).flatMap(parseCredentials)
	}
}