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
package com.excilys.ebi.gatling.recorder.scenario

import java.nio.charset.Charset

import scala.collection.JavaConversions.{ asScalaBuffer, mapAsScalaMap }

import org.jboss.netty.handler.codec.http.{ HttpRequest, QueryStringDecoder }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.{ AUTHORIZATION, CONTENT_TYPE }

import com.excilys.ebi.gatling.http.util.HttpHelper.parseFormBody
import com.excilys.ebi.gatling.recorder.config.Configuration.configuration
import com.ning.http.util.Base64

import grizzled.slf4j.Logging

sealed trait RequestBody
case class RequestBodyParams(params: List[(String, String)]) extends RequestBody
case class RequestBodyBytes(bytes: Array[Byte]) extends RequestBody

object RequestElement {
	def apply(r: RequestElement, newStatusCode: Int) = {
		new RequestElement(r.request, newStatusCode, r.simulationClass)
	}

	def apply(r: RequestElement, simulationClass: String) = {
		new RequestElement(r.request, r.statusCode, Some(simulationClass))
	}
}

class RequestElement(val request: HttpRequest, val statusCode: Int, val simulationClass: Option[String]) extends ScenarioElement with Logging {
	val method = request.getMethod.toString

	private val uriParts = request.getUri.split("/", 4)
	val baseUrl = uriParts.take(3).mkString("/")
	val path = "/" + uriParts.lift(3).getOrElse("").split("\\?")(0)
	private var printedUrl = baseUrl + path
	val completeUrl = request.getUri
	var filteredHeadersId: Option[Int] = None

	val headers: List[(String, String)] = request.getHeaders.map { entry => (entry.getKey, entry.getValue) }.toList

	val queryParams = convertParamsFromJavaToScala(new QueryStringDecoder(request.getUri, Charset.forName(configuration.encoding)).getParameters)

	val body: Option[RequestBody] = {

		val content = if (request.getContent.readableBytes > 0) {
			val bufferBytes = new Array[Byte](request.getContent.readableBytes)
			request.getContent.getBytes(request.getContent.readerIndex, bufferBytes)
			Some(bufferBytes)
		} else None

		content.map { bytes =>
			val containsFormParams = Option(request.getHeader(CONTENT_TYPE)).map(_.contains("application/x-www-form-urlencoded")).getOrElse(false)
			if (containsFormParams) RequestBodyParams(parseFormBody(new String(bytes, configuration.encoding)))
			else RequestBodyBytes(bytes)
		}
	}

	var id: Int = 0

	def setId(id: Int) {
		this.id = id
		this
	}

	def updateUrl(baseUrl: String): RequestElement = {
		if (baseUrl == this.baseUrl)
			this.printedUrl = path

		this
	}

	private def convertParamsFromJavaToScala(params: java.util.Map[String, java.util.List[String]]): List[(String, String)] = (for ((key, list) <- params) yield (for (e <- list) yield (key, e))).toList.flatten

	private val basicAuthCredentials: Option[(String, String)] = {
		Option(request.getHeader(AUTHORIZATION)) match {
			case Some(value) if (value.startsWith("Basic ")) =>
				val credentials = new String(Base64.decode(value.split(" ")(1))).split(":")
				if (credentials.length == 2)
					Some(credentials(0), credentials(1))
				else
					None
			case _ => None
		}
	}

	override def toString = {
		ScenarioExporter.TPL_ENGINE.layout("templates/request.ssp",
			Map("headersId" -> filteredHeadersId,
				"method" -> method,
				"printedUrl" -> printedUrl,
				"queryParams" -> queryParams,
				"body" -> body,
				"statusCode" -> statusCode,
				"id" -> id,
				"credentials" -> basicAuthCredentials,
				"simulationClass" -> simulationClass.getOrElse(throw new UnsupportedOperationException("simulationName should be set before printing a request element!"))))
	}
}