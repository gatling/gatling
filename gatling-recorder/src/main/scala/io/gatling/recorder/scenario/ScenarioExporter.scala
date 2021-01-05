/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.recorder.scenario

import java.io.{ File, IOException }
import java.nio.file.{ Path, Paths }
import java.util.Locale

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters._
import scala.util.Using

import io.gatling.commons.shared.unstable.util.PathHelper._
import io.gatling.commons.util.Io._
import io.gatling.commons.util.StringHelper._
import io.gatling.commons.validation._
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.har._
import io.gatling.recorder.scenario.template.SimulationTemplate
import io.gatling.recorder.util.HttpUtils._

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http._

private[recorder] object ScenarioExporter extends StrictLogging {

  private val EventsGrouping = 100

  private def packageAsFolderPath(separator: String)(implicit config: RecorderConfiguration) =
    config.core.pkg.replace(".", separator)

  private def classNameToFolderName(config: RecorderConfiguration): String =
    config.core.className.toLowerCase(Locale.ROOT)

  def simulationFilePath(implicit config: RecorderConfiguration): Path = {
    val path = config.core.simulationsFolder + File.separator + packageAsFolderPath(File.separator)
    getFolder(path) / s"${config.core.className}.scala"
  }

  private def resourcesFolderPath(implicit config: RecorderConfiguration): Path = {
    val path = config.core.resourcesFolder + File.separator + packageAsFolderPath(File.separator) + File.separator + classNameToFolderName(config)
    getFolder(path)
  }

  private def requestBodyFileName(request: RequestElement) =
    s"${request.id.toString.leftPad(4, "0")}_request.${request.fileExtension}"

  def requestBodyRelativeFilePath(request: RequestElement)(implicit config: RecorderConfiguration): String =
    packageAsFolderPath("/") + "/" + classNameToFolderName(config) + "/" + requestBodyFileName(request)

  private def responseBodyFileName(request: RequestElement) =
    s"${request.id.toString.leftPad(4, "0")}_response.${request.responseFileExtension}"

  def responseBodyRelativeFilePath(request: RequestElement)(implicit config: RecorderConfiguration): String =
    packageAsFolderPath("/") + "/" + classNameToFolderName(config) + "/" + responseBodyFileName(request)

  def exportScenario(harFilePath: String)(implicit config: RecorderConfiguration): Validation[Unit] =
    safely(error => s"Error while processing HAR file: $error") {
      val transactions = HarReader.readFile(harFilePath, config.filters.filters)

      if (transactions.isEmpty) {
        "the selected file doesn't contain any valid HTTP requests".failure
      } else {
        val scenarioElements = transactions.map { case HttpTransaction(request, response) =>
          val element = RequestElement(request, response)
          TimedScenarioElement(request.timestamp, response.timestamp, element)
        }

        ScenarioExporter.saveScenario(ScenarioDefinition(scenarioElements, tags = Nil)).success
      }
    }

  def saveScenario(scenarioElements: ScenarioDefinition)(implicit config: RecorderConfiguration): Unit = {
    require(!scenarioElements.isEmpty)

    val output = renderScenarioAndDumpBodies(scenarioElements)

    Using.resource(simulationFilePath.outputStream) {
      _.write(output.getBytes(config.core.encoding))
    }
  }

  private def renderScenarioAndDumpBodies(scenario: ScenarioDefinition)(implicit config: RecorderConfiguration): String = {
    // Aggregate headers
    val filteredHeaders = Set(HttpHeaderNames.COOKIE, HttpHeaderNames.CONTENT_LENGTH, HttpHeaderNames.HOST) ++
      (if (config.http.automaticReferer) Set(HttpHeaderNames.REFERER) else Set.empty)

    val scenarioElements = scenario.elements
    val mainRequestElements = scenarioElements.collect { case req: RequestElement => req }
    val requestElements = mainRequestElements.flatMap(req => req :: req.nonEmbeddedResources)
    requestElements.foreach { requestElement =>
      if (requestElement.headers.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE, true)) {
        requestElement.headers.remove(HttpHeaderNames.CONNECTION)
      }

      val authorizationHeaders = requestElement.headers.getAll(HttpHeaderNames.AUTHORIZATION)
      requestElement.headers.set(HttpHeaderNames.AUTHORIZATION, authorizationHeaders.asScala.filterNot(_.startsWith("Basic ")).asJava)
    }

    val baseUrl = getBaseUrl(mainRequestElements)
    val baseHeaders = getBaseHeaders(requestElements)
    val protocolConfigElement = new ProtocolDefinition(baseUrl, baseHeaders)

    // extract the request elements and set all the necessary
    val elements = scenarioElements.map {
      case reqEl: RequestElement =>
        reqEl.nonEmbeddedResources.foreach(_.makeRelativeTo(baseUrl))
        reqEl.makeRelativeTo(baseUrl)
      case el => el
    }

    // FIXME mutability!!!
    requestElements.zipWithIndex.map { case (reqEl, index) => reqEl.setId(index) }

    // dump request & response bodies if needed
    requestElements.foreach(el =>
      el.body.foreach {
        case RequestBodyBytes(bytes) => dumpBody(requestBodyFileName(el), bytes)
        case _                       =>
      }
    )

    if (config.http.checkResponseBodies) {
      requestElements.foreach(el =>
        el.responseBody.foreach {
          case ResponseBodyBytes(bytes) => dumpBody(responseBodyFileName(el), bytes)
          case _                        =>
        }
      )
    }

    val headers: Map[Int, Seq[(String, String)]] = {

      @tailrec
      def generateHeaders(elements: Seq[RequestElement], headers: Map[Int, List[(String, String)]]): Map[Int, List[(String, String)]] = elements match {
        case Seq() => headers
        case element +: others =>
          val acceptedHeaders = element.headers.entries.asScala
            .map(e => e.getKey -> e.getValue)
            .toList
            .filterNot { case (headerName, headerValue) =>
              val isFiltered = containsIgnoreCase(filteredHeaders, headerName) || isHttp2PseudoHeader(headerName)
              val isAlreadyInBaseHeaders = getIgnoreCase(baseHeaders, headerName).contains(headerValue)
              val isPostWithFormParams = element.method == HttpMethod.POST.name && HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED
                .contentEqualsIgnoreCase(headerValue)
              val isEmptyContentLength = HttpHeaderNames.CONTENT_LENGTH.contentEqualsIgnoreCase(headerName) && headerValue == "0"
              isFiltered || isAlreadyInBaseHeaders || isPostWithFormParams || isEmptyContentLength
            }
            .sortBy(_._1)

          val newHeaders = if (acceptedHeaders.isEmpty) {
            element.filteredHeadersId = None
            headers

          } else {
            val headersSeq = headers.toSeq
            headersSeq.indexWhere { case (_, existingHeaders) =>
              existingHeaders == acceptedHeaders
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

  private def getBaseHeaders(requestElements: Seq[RequestElement]): HttpHeaders = {

    def getMostFrequentHeaderValue(headerName: String): Option[String] = {
      val headers = requestElements.flatMap(_.headers.getAll(headerName).asScala)

      if (headers.isEmpty || headers.length != requestElements.length)
        // a header has to be defined on all requestElements to be turned into a common one
        None
      else {
        val headersValuesOccurrences = headers.groupBy(identity).view.mapValues(_.size).to(Seq)
        val mostFrequentValue = headersValuesOccurrences.maxBy(_._2)._1
        Some(mostFrequentValue)
      }
    }

    val baseHeaders = new DefaultHttpHeaders(false)
    ProtocolDefinition.BaseHeadersAndProtocolMethods.names.asScala.foreach { headerName =>
      getMostFrequentHeaderValue(headerName) match {
        case Some(mostFrequentValue) => baseHeaders.add(headerName, mostFrequentValue)
        case _                       =>
      }
    }

    baseHeaders
  }

  private def getBaseUrl(requestElements: Seq[RequestElement]): String = {
    val urlsOccurrences = requestElements.map(_.baseUrl).groupBy(identity).view.mapValues(_.size).to(Seq)

    urlsOccurrences.maxBy(_._2)._1
  }

  private def getChains(scenarioElements: Seq[ScenarioElement]): Either[Seq[ScenarioElement], List[Seq[ScenarioElement]]] =
    if (scenarioElements.size > ScenarioExporter.EventsGrouping)
      Right(scenarioElements.grouped(ScenarioExporter.EventsGrouping).toList)
    else
      Left(scenarioElements)

  private def dumpBody(fileName: String, content: Array[Byte])(implicit config: RecorderConfiguration): Unit = {
    Using.resource((resourcesFolderPath / fileName).outputStream) { fw =>
      try {
        fw.write(content)
      } catch {
        case e: IOException => logger.error(s"Error, while dumping body $fileName...", e)
      }
    }
  }
  private def getFolder(folderPath: String): Path = Paths.get(folderPath).mkdirs()
}
