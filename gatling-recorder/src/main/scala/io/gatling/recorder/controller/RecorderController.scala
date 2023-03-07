/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.controller

import java.nio.file.{ Files, Paths }
import java.util.concurrent.ConcurrentLinkedQueue

import scala.concurrent.duration.DurationLong
import scala.jdk.CollectionConverters._

import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.filter.Filters
import io.gatling.http.client.uri.Uri
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.config.RecorderMode._
import io.gatling.recorder.http.Mitm
import io.gatling.recorder.model._
import io.gatling.recorder.render._
import io.gatling.recorder.ui._

import com.typesafe.scalalogging.StrictLogging

private[recorder] class RecorderController(clock: Clock) extends StrictLogging {
  private val frontEnd = RecorderFrontEnd.newFrontend(this, RecorderConfiguration.recorderConfiguration)

  private var mitm: Mitm = _
  private var converter: HttpTrafficConverter = _
  private val requests = new ConcurrentLinkedQueue[TimedScenarioElement[RequestElement]]()
  private val tags = new ConcurrentLinkedQueue[TimedScenarioElement[TagElement]]()

  frontEnd.init()

  def startRecording(): Unit = {
    val config = RecorderConfiguration.recorderConfiguration
    converter = new HttpTrafficConverter(config)
    val selectedMode = frontEnd.selectedRecorderMode
    val harFilePath = frontEnd.harFilePath
    val harFile = Paths.get(harFilePath)
    if (selectedMode == Har && !Files.exists(harFile)) {
      frontEnd.handleMissingHarFile(harFilePath)
    } else {
      val proceed = if (converter.simulationFileExists) frontEnd.askSimulationOverwrite else true
      if (proceed) {
        selectedMode match {
          case Har =>
            converter.renderHarFile(harFile) match {
              case Failure(errMsg) => frontEnd.handleHarExportFailure(errMsg)
              case _               => frontEnd.handleHarExportSuccess()
            }
          case Proxy =>
            mitm = Mitm(this, clock, config)
            frontEnd.recordingStarted()
        }
      }
    }
  }

  def stopRecording(save: Boolean): Unit =
    try {
      frontEnd.recordingStopped()
      if (save) {
        if (requests.isEmpty) {
          logger.info("Nothing was recorded, skipping Simulation generation")
        } else {
          val config = RecorderConfiguration.recorderConfiguration
          val traffic = HttpTraffic(requests.asScala.toList, tags.asScala.toList, config)
          converter.renderHttpTraffic(traffic)
        }
      }
    } finally {
      mitm.shutdown()
      clearRecorderState()
      frontEnd.init()
    }

  def receiveResponse(request: HttpRequest, response: HttpResponse): Unit = {
    val config = RecorderConfiguration.recorderConfiguration
    if (
      config.filters.filters.forall(_.accept(request.uri))
      && Filters.BrowserNoiseFilters.accept(request.uri)
    ) {
      requests.add(TimedScenarioElement(request.timestamp, response.timestamp, RequestElement(request, response, config)))

      // Notify frontend
      val previousSendTime = requests.asScala.lastOption.map(_.sendTime)
      previousSendTime.foreach { t =>
        val delta = (response.timestamp - t).milliseconds
        if (delta > config.core.thresholdForPauseCreation)
          frontEnd.receiveEvent(PauseFrontEndEvent(delta))
      }
      frontEnd.receiveEvent(RequestFrontEndEvent(request, response, config))
    }
  }

  def addTag(text: String): Unit = {
    val now = clock.nowMillis
    tags.add(TimedScenarioElement(now, now, TagElement(text)))
    frontEnd.receiveEvent(TagFrontEndEvent(text))
  }

  def secureConnection(securedHostURI: Uri): Unit =
    frontEnd.receiveEvent(SslFrontEndEvent(securedHostURI.toUrl))

  def clearRecorderState(): Unit = {
    requests.clear()
    tags.clear()
  }
}
