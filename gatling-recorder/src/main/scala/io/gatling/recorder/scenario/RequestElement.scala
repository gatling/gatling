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

import java.net.URI
import java.nio.charset.Charset

import scala.collection.JavaConversions.{ asScalaBuffer, mapAsScalaMap }

import org.jboss.netty.handler.codec.http.{ HttpRequest, QueryStringDecoder }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.{ AUTHORIZATION, CONTENT_TYPE }
import org.jboss.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED

import com.ning.http.util.Base64

import io.gatling.http.util.HttpHelper.parseFormBody
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.scenario.template.RequestTemplate

object RequestElement {

	def apply(request: HttpRequest, statusCode: Int, simulationClass: Option[String]): RequestElement = {
		val headers: Map[String, String] = request.getHeaders.map { entry => (entry.getKey, entry.getValue) }.toMap
		val content = if (request.getContent.readableBytes > 0) {
			val bufferBytes = new Array[Byte](request.getContent.readableBytes)
			request.getContent.getBytes(request.getContent.readerIndex, bufferBytes)
			Some(new String(bufferBytes, configuration.core.encoding))
		} else None
		
		RequestElement(new URI(request.getUri), request.getMethod.toString, headers, content, statusCode, simulationClass)
	}
}

case class RequestElement(uri: URI, method: String, headers: Map[String, String], content: Option[String], statusCode: Int, simulationClass: Option[String]) extends ScenarioElement {

	private val containsFormParams: Boolean = headers.get(CONTENT_TYPE).map(_.contains(APPLICATION_X_WWW_FORM_URLENCODED)).getOrElse(false)

	val baseUrl = new URI(uri.getScheme, uri.getAuthority, null, null, null).toString
	private var printedUrl = new URI(uri.getScheme, uri.getHost, uri.getPath, null, null).toString

	var filteredHeadersId: Option[Int] = None

	val queryParams = if (uri.getQuery != null) convertParamsFromJavaToScala(new QueryStringDecoder(uri, Charset.forName(configuration.core.encoding)).getParameters) else Nil

	val requestBodyOrParams: Option[Either[String, List[(String, String)]]] = content.map(content => if (containsFormParams) Right(parseFormBody(content)) else Left(content))

	var id: Int = 0

	def setId(id: Int) {
		this.id = id
	}

	def makeRelativeTo(baseUrl: String): RequestElement = {
		if (baseUrl == this.baseUrl)
			this.printedUrl = Option(uri.getPath).map {
				case s if s.startsWith("/") => s
				case s => "/" + s
			}.getOrElse("/")

		this
	}

	private def convertParamsFromJavaToScala(params: java.util.Map[String, java.util.List[String]]): List[(String, String)] =
		(for ((key, list) <- params) yield (for (e <- list) yield (key, e))).toList.flatten

	private val basicAuthCredentials: Option[(String, String)] = {
		def parseCredentials(header: String) = {
			val credentials = new String(Base64.decode(header.split(" ")(1))).split(":")
			if (credentials.length == 2)
				Some(credentials(0), credentials(1))
			else
				None
		}

		headers.get(AUTHORIZATION).filter(_.startsWith("Basic ")).flatMap(parseCredentials)
	}

	override def toString = {
		RequestTemplate.render(
			simulationClass.getOrElse(throw new UnsupportedOperationException("simulationName should be set before printing a request element!")),
			id,
			method,
			printedUrl,
			filteredHeadersId,
			basicAuthCredentials,
			queryParams,
			requestBodyOrParams,
			statusCode)
	}
}