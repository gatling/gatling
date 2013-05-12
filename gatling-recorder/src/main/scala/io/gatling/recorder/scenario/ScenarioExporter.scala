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

import java.io.{ FileOutputStream, IOException }

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.tools.nsc.io.{ Directory, File }

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.http.Headers
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.scenario.template.SimulationTemplate

object ScenarioExporter extends Logging {
	private val EVENTS_GROUPING = 100

	def saveScenario(scenarioElements: List[ScenarioElement]) {

		def getBaseUrl(scenarioElements: List[ScenarioElement]): String = {
			val baseUrls = scenarioElements.collect {
				case reqElm: RequestElement => reqElm.baseUrl
			}.groupBy(identity).toSeq

			baseUrls.maxBy {
				case (url, occurrences) => occurrences.size
			}._1
		}

		def getMostFrequentHeaderValue(scenarioElements: List[ScenarioElement], headerName: String): Option[String] = {
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

		def getChains(scenarioElements: List[ScenarioElement]): Either[List[ScenarioElement], List[List[ScenarioElement]]] = {

			if (scenarioElements.size > ScenarioExporter.EVENTS_GROUPING)
				Right(scenarioElements.grouped(ScenarioExporter.EVENTS_GROUPING).toList)
			else
				Left(scenarioElements)
		}

		def dumpRequestBody(idEvent: Int, content: String, simulationClass: String) {
			withCloseable(new FileOutputStream(File(getFolder(configuration.core.requestBodiesFolder) / simulationClass + "_request_" + idEvent + ".txt").jfile)) {
				fw =>
					try {
						fw.write(content.getBytes(configuration.core.encoding))
					} catch {
						case e: IOException => logger.error("Error, while dumping request body...", e)
					}
			}
		}

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

			resolveBaseHeaders(Map.empty, ProtocolElement.baseHeaders.keySet.toList)
		}

		val protocolConfigElement = new ProtocolElement(baseUrl, configuration.http.followRedirect, configuration.http.automaticReferer, baseHeaders)

		// Add simulationClass to request elements
		val elementsList: List[ScenarioElement] = scenarioElements.map {
			case e: RequestElement => e.copy(simulationClass = Some(configuration.core.className))
			case e => e
		}

		// Updates URLs that contain baseUrl, set ids on requests and dump request body if needed
		var i = 0
		elementsList.foreach {
			case e: RequestElement =>
				i = i + 1
				e.makeRelativeTo(baseUrl).setId(i)
				e.requestBodyOrParams.map(_.left.map(dumpRequestBody(i, _, configuration.core.className)))

			case _ =>
		}

		// Aggregate headers
		val filteredHeaders = Set(Headers.Names.COOKIE, Headers.Names.CONTENT_LENGTH, Headers.Names.HOST) ++ (if (configuration.http.automaticReferer) Set(Headers.Names.REFERER) else Set.empty)

		val headers: Map[Int, List[(String, String)]] = {

			@tailrec
			def generateHeaders(elements: List[RequestElement], headers: Map[Int, List[(String, String)]]): Map[Int, List[(String, String)]] = elements match {
				case Nil => headers
				case element :: others => {
					val acceptedHeaders = element.headers.toList
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

		val output = SimulationTemplate.render(configuration.core.pkg, configuration.core.className, protocolConfigElement, headers, "Scenario Name", newScenarioElements)

		withCloseable(new FileOutputStream(File(getOutputFolder / getSimulationFileName).jfile)) {
			_.write(output.getBytes(configuration.core.encoding))
		}
	}

	def getSimulationFileName: String = configuration.core.className + ".scala"

	def getOutputFolder = {
		val path = configuration.core.outputFolder + File.separator + configuration.core.pkg.replace(".", File.separator)
		getFolder(path)
	}

	private def getFolder(folderPath: String) = Directory(folderPath).createDirectory()
}