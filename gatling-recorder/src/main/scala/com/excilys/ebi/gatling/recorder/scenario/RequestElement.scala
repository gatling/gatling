/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.net.URI

import scala.collection.JavaConversions.{ mapAsScalaMap, asScalaBuffer }

import org.jboss.netty.handler.codec.http.HttpHeaders.Names.{ CONTENT_TYPE, AUTHORIZATION }
import org.jboss.netty.handler.codec.http.{ QueryStringDecoder, HttpRequest }

import com.ning.http.util.Base64

import grizzled.slf4j.Logging

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

	private val containsFormParams: Boolean = Option(request.getHeader(CONTENT_TYPE)).map(_.contains("application/x-www-form-urlencoded")).getOrElse(false)

	val requestBody = if (request.getContent.capacity > 0 && !containsFormParams) Some(new String(request.getContent.array)) else None

	private val uri = URI.create(request.getUri)
	private var printedUrl = baseUrl + uri.getPath
	val completeUrl = request.getUri

	val headers: List[(String, String)] = request.getHeaders.map { entry => (entry.getKey, entry.getValue) }.toList

	val queryParams = convertParamsFromJavaToScala(new QueryStringDecoder(request.getUri).getParameters)

	val params: List[(String, String)] =
		if (request.getContent.capacity > 0 && containsFormParams) {
			val paramDecoder = new QueryStringDecoder("http://localhost/?" + new String(request.getContent.array))
			convertParamsFromJavaToScala(paramDecoder.getParameters)
		} else
			Nil

	val baseUrl = uri.getScheme() + "://" + uri.getAuthority

	var id: Int = 0

	def setId(id: Int) {
		this.id = id
		this
	}

	def updateUrl(baseUrl: String): RequestElement = {
		if (baseUrl == this.baseUrl)
			this.printedUrl = uri.getPath

		this
	}

	private def convertParamsFromJavaToScala(params: java.util.Map[String, java.util.List[String]]): List[(String, String)] = {
		(for ((key, list) <- params) yield (for (e <- list) yield (key, e))).toList.flatten
	}

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
			Map("method" -> method,
				"printedUrl" -> printedUrl,
				"queryParams" -> queryParams,
				"params" -> params,
				"hasBody" -> requestBody.isDefined,
				"statusCode" -> statusCode,
				"id" -> id,
				"credentials" -> basicAuthCredentials,
				"simulationClass" -> simulationClass.getOrElse(throw new UnsupportedOperationException("simulationName should be set before printing a request element!"))))
	}
}