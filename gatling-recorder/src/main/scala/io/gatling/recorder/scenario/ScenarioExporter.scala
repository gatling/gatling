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

import java.io.{ FileOutputStream, IOException }

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.reflect.io.Path.string2path
import scala.tools.nsc.io.{ Directory, File }

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.util.IO._
import io.gatling.core.validation._
import io.gatling.http.HeaderNames
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.har.HarReader
import io.gatling.recorder.scenario.template.SimulationTemplate

object ScenarioExporter extends StrictLogging {

  private val EventsGrouping = 100

  def simulationFilePath(implicit config: RecorderConfiguration) = {
      def getSimulationFileName: String = s"${config.core.className}.scala"
      def getOutputFolder = {
        val path = config.core.outputFolder + File.separator + config.core.pkg.replace(".", File.separator)
        getFolder(path)
      }

    getOutputFolder / getSimulationFileName
  }

  def exportScenario(harFilePath: String)(implicit config: RecorderConfiguration): Validation[Unit] =
    try {
      val har = HarReader(harFilePath)
      if (har.elements.isEmpty) {
        "the selected file doesn't contain any valid HTTP requests".failure
      } else {
        ScenarioExporter.saveScenario(har).success
      }
    } catch {
      case e: Exception =>
        logger.error("Error while processing HAR file", e)
        e.getMessage.failure
    }

  def saveScenario(scenarioElements: ScenarioDefinition)(implicit config: RecorderConfiguration): Unit = {
    require(!scenarioElements.isEmpty)

    val output = renderScenarioAndDumpBodies(scenarioElements)

    withCloseable(new FileOutputStream(File(simulationFilePath).jfile)) {
      _.write(output.getBytes(config.core.encoding))
    }
  }

  private def renderScenarioAndDumpBodies(scenario: ScenarioDefinition)(implicit config: RecorderConfiguration): String = {
    // Aggregate headers
    val filteredHeaders = Set(HeaderNames.Cookie, HeaderNames.ContentLength, HeaderNames.Host) ++
      (if (config.http.automaticReferer) Set(HeaderNames.Referer) else Set.empty)

    val scenarioElements = scenario.elements
    val requestElements = scenarioElements.collect { case req: RequestElement => req :: req.nonEmbeddedResources }.flatten
    val baseUrl = getBaseUrl(requestElements)
    val baseHeaders = getBaseHeaders(requestElements)
    val protocolConfigElement = new ProtocolDefinition(baseUrl, baseHeaders)

    // extract the request elements and set all the necessary
    val elements = scenarioElements.map {
      case reqEl: RequestElement => reqEl.makeRelativeTo(baseUrl)
      case el                    => el
    }

    // FIXME mutability!!!
    requestElements.zipWithIndex.map { case (reqEl, index) => reqEl.setId(index) }

    // dump request body if needed
    requestElements.foreach(el => el.body.foreach {
      case RequestBodyBytes(bytes) => dumpRequestBody(el.id, bytes, config.core.className)
      case _                       =>
    })

    val headers: Map[Int, Seq[(String, String)]] = {

        @tailrec
        def generateHeaders(elements: Seq[RequestElement], headers: Map[Int, List[(String, String)]]): Map[Int, List[(String, String)]] = elements match {
          case Seq() => headers
          case element +: others =>
            val acceptedHeaders = element.headers.toList
              .filterNot {
                case (headerName, headerValue) => filteredHeaders.contains(headerName) || baseHeaders.get(headerName).exists(_ == headerValue)
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

      SortedMap(generateHeaders(requestElements, Map.empty).toSeq: _*)
    }

    val newScenarioElements = getChains(elements)

    SimulationTemplate.render(config.core.pkg, config.core.className, protocolConfigElement, headers, config.core.className, newScenarioElements)
  }

  private def getBaseHeaders(requestElements: Seq[RequestElement]): Map[String, String] = {

      def getMostFrequentHeaderValue(headerName: String): Option[String] = {
        val headers = requestElements.flatMap {
          _.headers.collect { case (name, value) if name == headerName => value }
        }

        if (headers.isEmpty) None
        else {
          val headersValuesOccurrences = headers.groupBy(identity).mapValues(_.size).toSeq
          val mostFrequentValue = headersValuesOccurrences.maxBy(_._2)._1
          Some(mostFrequentValue)
        }
      }

      def addHeader(appendTo: Map[String, String], headerName: String): Map[String, String] =
        getMostFrequentHeaderValue(headerName)
          .map(headerValue => appendTo + (headerName -> headerValue))
          .getOrElse(appendTo)

      @tailrec
      def resolveBaseHeaders(headers: Map[String, String], headerNames: List[String]): Map[String, String] = headerNames match {
        case Nil                  => headers
        case headerName :: others => resolveBaseHeaders(addHeader(headers, headerName), others)
      }

    resolveBaseHeaders(Map.empty, ProtocolDefinition.baseHeaders.keySet.toList)
  }

  private def getBaseUrl(requestElements: Seq[RequestElement]): String = {
    val urlsOccurrences = requestElements.map(_.baseUrl).groupBy(identity).mapValues(_.size).toSeq

    urlsOccurrences.maxBy(_._2)._1
  }

  private def getChains(scenarioElements: Seq[ScenarioElement]): Either[Seq[ScenarioElement], List[Seq[ScenarioElement]]] =
    if (scenarioElements.size > ScenarioExporter.EventsGrouping)
      Right(scenarioElements.grouped(ScenarioExporter.EventsGrouping).toList)
    else
      Left(scenarioElements)

  private def dumpRequestBody(idEvent: Int, content: Array[Byte], simulationClass: String)(implicit config: RecorderConfiguration): Unit = {
    val fileName = s"${simulationClass}_request_$idEvent.txt"
    withCloseable(File(getFolder(config.core.requestBodiesFolder) / fileName).outputStream()) { fw =>
      try {
        fw.write(content)
      } catch {
        case e: IOException => logger.error("Error, while dumping request body...", e)
      }
    }
  }

  private def getFolder(folderPath: String) = Directory(folderPath).createDirectory()
}
