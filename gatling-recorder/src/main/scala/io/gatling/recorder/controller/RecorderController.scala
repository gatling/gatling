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

package io.gatling.recorder.controller

import java.nio.file.Paths
import java.util.concurrent.ConcurrentLinkedQueue

import scala.concurrent.duration.DurationLong
import scala.jdk.CollectionConverters._

import io.gatling.commons.shared.unstable.util.PathHelper._
import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.filter.Filters
import io.gatling.http.client.uri.Uri
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.config.RecorderMode._
import io.gatling.recorder.http.Mitm
import io.gatling.recorder.model._
import io.gatling.recorder.scenario._
import io.gatling.recorder.ui._

import com.typesafe.scalalogging.StrictLogging

private[recorder] class RecorderController(clock: Clock) extends StrictLogging {

  private val frontEnd = RecorderFrontEnd.newFrontend(this)

  @volatile private var mitm: Mitm = _

  private val requests = new ConcurrentLinkedQueue[TimedScenarioElement[RequestElement]]()
  private val tags = new ConcurrentLinkedQueue[TimedScenarioElement[TagElement]]()

  frontEnd.init()

  def startRecording(): Unit = {
    val selectedMode = frontEnd.selectedRecorderMode
    val harFilePath = frontEnd.harFilePath
    if (selectedMode == Har && !Paths.get(harFilePath).exists) {
      frontEnd.handleMissingHarFile(harFilePath)
    } else {
      val simulationFile = ScenarioExporter.simulationFilePath
      val proceed = if (simulationFile.exists) frontEnd.askSimulationOverwrite else true
      if (proceed) {
        selectedMode match {
          case Har =>
            ScenarioExporter.exportScenario(harFilePath) match {
              case Failure(errMsg) => frontEnd.handleHarExportFailure(errMsg)
              case _               => frontEnd.handleHarExportSuccess()
            }
          case Proxy =>
            mitm = Mitm(this, clock, RecorderConfiguration.configuration)
            frontEnd.recordingStarted()
        }
      }
    }
  }

  def stopRecording(save: Boolean): Unit = {
    frontEnd.recordingStopped()
    try {
      if (requests.isEmpty)
        logger.info("Nothing was recorded, skipping scenario generation")
      else {
        val scenario = ScenarioDefinition(requests.asScala.toVector, tags.asScala.toVector)
        ScenarioExporter.saveScenario(scenario)
      }

    } finally {
      mitm.shutdown()
      clearRecorderState()
      frontEnd.init()
    }
  }

  def receiveResponse(request: HttpRequest, response: HttpResponse): Unit =
    if (
      RecorderConfiguration.configuration.filters.filters.forall(_.accept(request.uri))
      && Filters.BrowserNoiseFilters.accept(request.uri)
    ) {
      requests.add(TimedScenarioElement(request.timestamp, response.timestamp, RequestElement(request, response)))

      // Notify frontend
      val previousSendTime = requests.asScala.lastOption.map(_.sendTime)
      previousSendTime.foreach { t =>
        val delta = (response.timestamp - t).milliseconds
        if (delta > RecorderConfiguration.configuration.core.thresholdForPauseCreation)
          frontEnd.receiveEvent(PauseFrontEndEvent(delta))
      }
      frontEnd.receiveEvent(RequestFrontEndEvent(request, response))
    }

  def addTag(text: String): Unit = {
    val now = clock.nowMillis
    tags.add(TimedScenarioElement(now, now, TagElement(text)))
    frontEnd.receiveEvent(TagFrontEndEvent(text))
  }

  def secureConnection(securedHostURI: Uri): Unit = {
    frontEnd.receiveEvent(SslFrontEndEvent(securedHostURI.toUrl))
  }

  def clearRecorderState(): Unit = {
    requests.clear()
    tags.clear()
  }
}
