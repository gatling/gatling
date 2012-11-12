/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.recorder.scenario

import java.io.{ FileWriter, IOException }
import java.util.Date

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.tools.nsc.io.{ Directory, File }

import org.fusesource.scalate.TemplateEngine

import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.Headers
import com.excilys.ebi.gatling.recorder.config.Configuration.configuration

import grizzled.slf4j.Logging

object ScenarioExporter extends Logging {
	private val EVENTS_GROUPING = 100

	val TPL_ENGINE = {
		val engine = new TemplateEngine
		engine.allowReload = false
		engine.escapeMarkup = false
		engine
	}

	def saveScenario(startDate: Date, scenarioElements: List[ScenarioElement]) = {

		val baseUrl = getBaseUrl(scenarioElements)

		val baseHeaders: Map[String, String] = {

			def addHeader(appendTo: Map[String, String], headerName: String): Map[String, String] =
				getMostFrequentHeaderValue(scenarioElements, headerName)
					.map(headerValue => appendTo + (headerName -> headerValue))
					.getOrElse(appendTo)

			@tailrec
			def resolveBaseHeaders(headers: Map[String, String], headerNames: List[String]): Map[String, String] = headerNames match {
				case Nil => headers
				case headerName :: others => resolveBaseHeaders(addHeader(headers, headerName), others)
			}

			resolveBaseHeaders(Map.empty, List(Headers.Names.ACCEPT, Headers.Names.ACCEPT_CHARSET, Headers.Names.ACCEPT_ENCODING, Headers.Names.ACCEPT_LANGUAGE, Headers.Names.AUTHORIZATION, Headers.Names.DO_NOT_TRACK, Headers.Names.USER_AGENT))
		}

		val protocolConfigElement = new ProtocolConfigElement(baseUrl, configuration.proxy, configuration.followRedirect, configuration.automaticReferer, baseHeaders)

		// Add simulationClass to request elements
		val elementsList: List[ScenarioElement] = scenarioElements.map {
			case e: RequestElement => RequestElement(e, configuration.simulationClassName)
			case e => e
		}

		// Updates URLs that contain baseUrl, set ids on requests and dump request body if needed
		var i = 0
		elementsList.foreach {
			case e: RequestElement =>
				i = i + 1
				e.updateUrl(baseUrl).setId(i)
				e.requestBodyOrParams.map(_.left.map(dumpRequestBody(i, _, configuration.simulationClassName)))

			case _ =>
		}

		// Aggregate headers
		val filteredHeaders = Set(Headers.Names.COOKIE, Headers.Names.CONTENT_LENGTH, Headers.Names.HOST) ++ (if (configuration.automaticReferer) Set(Headers.Names.REFERER) else Set.empty)

		val headers: Map[Int, List[(String, String)]] = {

			@tailrec
			def generateHeaders(elements: List[RequestElement], headers: Map[Int, List[(String, String)]]): Map[Int, List[(String, String)]] = elements match {
				case Nil => headers
				case element :: others => {
					val acceptedHeaders = element.headers
						.filterNot {
							case (headerName, headerValue) => filteredHeaders.contains(headerName) || baseHeaders.get(headerName).map(baseValue => baseValue == headerValue).getOrElse(false)
						}
						.sortBy(_._1)

					val newHeaders = if (acceptedHeaders.isEmpty) {
						element.filteredHeadersId = None
						headers

					} else {
						val headersSeq = headers.toSeq
						headersSeq.indexWhere {
							case (id, existingHeaders) => existingHeaders == acceptedHeaders
						} match {
							case -1 =>
								element.filteredHeadersId = Some(element.id)
								headers + (element.id -> acceptedHeaders)
							case index =>
								element.filteredHeadersId = Some(headersSeq(index)._1)
								headers
						}
					}

					generateHeaders(others, newHeaders)
				}
			}

			val requestElements = elementsList.flatMap {
				case request: RequestElement => Some(request)
				case _ => None
			}

			SortedMap(generateHeaders(requestElements, Map.empty).toSeq: _*)
		}

		val newScenarioElements = getChains(elementsList)

		val output = ScenarioExporter.TPL_ENGINE.layout("templates/simulation.ssp",
			Map("protocolConfig" -> protocolConfigElement,
				"headers" -> headers,
				"simulationClassName" -> configuration.simulationClassName,
				"scenarioName" -> "Scenario Name",
				"packageName" -> configuration.simulationPackage,
				"scenarioElements" -> newScenarioElements))

		use(new FileWriter(File(getOutputFolder / getSimulationFileName(startDate)).jfile)) {
			_.write(output)
		}
	}

	private def getBaseUrl(scenarioElements: List[ScenarioElement]): String = {
		val baseUrls = scenarioElements.flatMap {
			case reqElm: RequestElement => Some(reqElm.baseUrl)
			case _ => None
		}.groupBy(url => url).toSeq

		baseUrls.maxBy {
			case (url, occurrences) => occurrences.size
		}._1
	}

	private def getMostFrequentHeaderValue(scenarioElements: List[ScenarioElement], headerName: String): Option[String] = {
		val headers = scenarioElements.flatMap {
			case reqElm: RequestElement => reqElm.headers.collect { case (name, value) if (name == headerName) => value }
			case _ => Nil
		}

		headers match {
			case Nil => None
			case _ =>
				val mostFrequentValue = headers
					.groupBy(value => value)
					.maxBy { case (_, occurrences) => occurrences.size }
					._1
				Some(mostFrequentValue)
		}
	}

	private def getChains(scenarioElements: List[ScenarioElement]): Either[List[ScenarioElement], List[List[ScenarioElement]]] = {

		if (scenarioElements.size > ScenarioExporter.EVENTS_GROUPING)
			Right(scenarioElements.grouped(ScenarioExporter.EVENTS_GROUPING).toList)
		else
			Left(scenarioElements)
	}

	private def dumpRequestBody(idEvent: Int, content: String, simulationClass: String) {
		use(new FileWriter(File(getFolder(configuration.requestBodiesFolder) / simulationClass + "_request_" + idEvent + ".txt").jfile)) {
			fw =>
				try {
					fw.write(content)
				} catch {
					case e: IOException => error("Error, while dumping request body...", e)
				}
		}
	}

	def getSimulationFileName(date: Date): String = configuration.simulationClassName + ".scala"

	def getOutputFolder = {
		val path = configuration.outputFolder + configuration.simulationPackage
			.map { pkg => File.separator + pkg.replace(".", File.separator) }
			.getOrElse(EMPTY)

		getFolder(path)
	}

	private def getFolder(folderPath: String) = Directory(folderPath).createDirectory()
}