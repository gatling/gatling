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

package io.gatling.recorder.convert

import java.io.{ File, IOException }
import java.nio.file.{ Path, Paths }
import java.util.Locale

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters._
import scala.util.Using

import io.gatling.commons.shared.unstable.util.PathHelper._
import io.gatling.commons.util.StringHelper._
import io.gatling.commons.validation._
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.convert.template.SimulationTemplate
import io.gatling.recorder.har._
import io.gatling.recorder.util.HttpUtils._

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http._

private[convert] object DumpedBodies {

  private[convert] def apply(config: RecorderConfiguration): DumpedBodies = {
    val classNameAsFolderName = config.core.className.toLowerCase(Locale.ROOT)

    val bodiesFolderPath: Path = {
      val path = config.core.resourcesFolder + File.separator + config.core.pkg.replace(".", File.separator) + File.separator + classNameAsFolderName
      Paths.get(path).mkdirs()
    }

    val bodiesClassPathLocation: String = {
      val folderPath = config.core.pkg.replace(".", "/")
      (if (folderPath.isEmpty) "" else (folderPath + "/")) + classNameAsFolderName
    }

    new DumpedBodies(bodiesFolderPath, bodiesClassPathLocation)
  }
}

private[convert] class DumpedBodies(
    bodiesFolderPath: Path,
    bodiesClassPathLocation: String
) {
  def forRequest(request: RequestElement, bytes: Array[Byte]): DumpedBody =
    make(request, bytes, "request")

  def forResponse(request: RequestElement, bytes: Array[Byte]): DumpedBody =
    make(request, bytes, "response")

  private def make(request: RequestElement, bytes: Array[Byte], suffix: String): DumpedBody = {
    val fileName = s"${request.id.toString.leftPad(4, "0")}_$suffix.${request.responseFileExtension}"

    new DumpedBody(
      bodiesClassPathLocation + "/" + fileName,
      bodiesFolderPath / fileName,
      bytes
    )
  }
}

private[convert] class DumpedBody(
    val classPathLocation: String,
    val filePath: Path,
    val bytes: Array[Byte]
)

private[recorder] class HttpTrafficConverter(config: RecorderConfiguration) extends StrictLogging {

  private val simulationFile: Path = {
    val sourcesFolderPath = Paths.get(config.core.simulationsFolder + File.separator + config.core.pkg.replace(".", File.separator)).mkdirs()
    sourcesFolderPath / s"${config.core.className}.scala"
  }

  def simulationFileExists: Boolean = simulationFile.exists

  private def dumpBody(body: DumpedBody): Unit = {
    Using.resource(body.filePath.outputStream) { fw =>
      try {
        fw.write(body.bytes)
      } catch {
        case e: IOException => logger.error(s"Failed to dump body ${body.filePath}", e)
      }
    }
  }

  // RecorderController
  def convertHarFile(harFile: Path): Validation[Unit] =
    safely(error => s"Error while processing HAR file: $error") {
      val transactions = HarReader.readFile(harFile, config.filters.filters)

      if (transactions.isEmpty) {
        "the selected file doesn't contain any valid HTTP requests".failure
      } else {
        val scenarioElements = transactions.map { case HttpTransaction(request, response) =>
          val element = RequestElement(request, response, config)
          TimedScenarioElement(request.timestamp, response.timestamp, element)
        }

        convertHttpTraffic(HttpTraffic(scenarioElements, tags = Nil, config)).success
      }
    }

  // RecorderController
  def convertHttpTraffic(scenarioElements: HttpTraffic): Unit = {
    require(!scenarioElements.isEmpty)

    val output = renderScenarioAndDumpBodies(scenarioElements)

    Using.resource(simulationFile.outputStream) {
      _.write(output.getBytes(config.core.encoding))
    }
  }

  private def renderScenarioAndDumpBodies(scenario: HttpTraffic): String = {
    // Aggregate headers
    val filteredHeaders = Set(HttpHeaderNames.COOKIE, HttpHeaderNames.CONTENT_LENGTH, HttpHeaderNames.HOST) ++
      (if (config.http.automaticReferer) Set(HttpHeaderNames.REFERER) else Set.empty)

    val scenarioElements = scenario.elements
    val mainRequestElements = scenarioElements.collect { case req: RequestElement => req }
    val requestElements = mainRequestElements.flatMap(req => req :: req.nonEmbeddedResources)
    // FIXME mutability!!!
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

    val dumpedBodies = DumpedBodies(config)

    val requestBodies: Map[Int, DumpedBody] = {
      for {
        request <- requestElements
        bytes <- request.body.collect { case RequestBodyBytes(bytes) => bytes }.toList
      } yield request.id -> dumpedBodies.forRequest(request, bytes)
    }.toMap

    val responseBodies: Map[Int, DumpedBody] =
      if (config.http.checkResponseBodies) {
        {
          for {
            request <- requestElements
            bytes <- request.responseBody.collect { case ResponseBodyBytes(bytes) => bytes }.toList
          } yield request.id -> dumpedBodies.forResponse(request, bytes)
        }.toMap
      } else {
        Map.empty
      }

    // dump
    requestBodies.values.foreach(dumpBody)
    responseBodies.values.foreach(dumpBody)

    val headers: Map[Int, Seq[(String, String)]] = {

      @tailrec
      def generateHeaders(elements: List[RequestElement], headers: Map[Int, List[(String, String)]]): Map[Int, List[(String, String)]] = elements match {
        case Nil => headers
        case element :: others =>
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

    SimulationTemplate(requestBodies, responseBodies, config).render(protocolConfigElement, headers, elements)
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
}
