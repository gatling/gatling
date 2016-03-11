/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConversions._
import scala.concurrent.duration.DurationLong

import io.gatling.commons.util.PathHelper._
import io.gatling.commons.util.TimeHelper._
import io.gatling.commons.validation._
import io.gatling.recorder.config.RecorderPropertiesBuilder
import io.gatling.recorder.config.RecorderMode._
import io.gatling.recorder.http.handler.remote.TimedHttpRequest
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.model.{ SafeHttpRequest, SafeHttpResponse }
import io.gatling.recorder.scenario._
import io.gatling.recorder.ui._

import com.typesafe.scalalogging.StrictLogging
import org.asynchttpclient.uri.Uri
import org.asynchttpclient.util.Base64
import io.netty.handler.codec.http.HttpHeaders.Names.PROXY_AUTHORIZATION

private[recorder] class RecorderController extends StrictLogging {

  private val frontEnd = RecorderFrontend.newFrontend(this)

  @volatile private var proxy: HttpProxy = _

  // Collection of tuples, (arrivalTime, request)
  private val currentRequests = new ConcurrentLinkedQueue[TimedScenarioElement[RequestElement]]()
  // Collection of tuples, (arrivalTime, tag)
  private val currentTags = new ConcurrentLinkedQueue[TimedScenarioElement[TagElement]]()

  frontEnd.init()

  def startRecording(): Unit = {
    val selectedMode = frontEnd.selectedRecorderMode
    val harFilePath = frontEnd.harFilePath
    if (selectedMode == Har && !string2path(harFilePath).exists) {
      frontEnd.handleMissingHarFile(harFilePath)
    } else {
      val simulationFile = ScenarioExporter.simulationFilePath
      val proceed = if (simulationFile.exists) frontEnd.askSimulationOverwrite else true
      if (proceed) {
        selectedMode match {
          case Har =>
            ScenarioExporter.exportScenario(harFilePath) match {
              case Failure(errMsg) => frontEnd.handleHarExportFailure(errMsg)
              case Success(_)      => frontEnd.handleHarExportSuccess()
            }
          case Proxy =>
            proxy = new HttpProxy(this)
            frontEnd.recordingStarted()
        }
      }
    }
  }

  def stopRecording(save: Boolean): Unit = {
    frontEnd.recordingStopped()
    try {
      if (currentRequests.isEmpty)
        logger.info("Nothing was recorded, skipping scenario generation")
      else {
        val scenario = ScenarioDefinition(currentRequests.toVector, currentTags.toVector)
        ScenarioExporter.saveScenario(scenario)
      }

    } finally {
      proxy.shutdown()
      clearRecorderState()
      frontEnd.init()
    }
  }

  def receiveRequest(request: SafeHttpRequest): Unit =
    // TODO NICO - that's not the appropriate place to synchronize !
    synchronized {
      // If Outgoing Proxy set, we record the credentials to use them when sending the request
      Option(request.headers.get(PROXY_AUTHORIZATION)).foreach {
        header =>
          // Split on " " and take 2nd group (Basic credentialsInBase64==)
          val credentials = new String(Base64.decode(header.split(" ")(1))).split(":")
          val props = new RecorderPropertiesBuilder
          props.proxyUsername(credentials(0))
          props.proxyPassword(credentials(1))
          RecorderConfiguration.reload(props.build)
      }
    }

  def receiveResponse(request: TimedHttpRequest, response: SafeHttpResponse): Unit =
    if (RecorderConfiguration.configuration.filters.filters.map(_.accept(request.httpRequest.uri)).getOrElse(true)) {
      val arrivalTime = nowMillis

      currentRequests.add(TimedScenarioElement(request.sendTime, arrivalTime, RequestElement(request.httpRequest, response)))

      // Notify the frontend
      val previousSendTime = currentRequests.lastOption.map(_.sendTime)
      previousSendTime.foreach { t =>
        val delta = (arrivalTime - t).milliseconds
        if (delta > RecorderConfiguration.configuration.core.thresholdForPauseCreation)
          frontEnd.receiveEventInfo(PauseInfo(delta))
      }
      frontEnd.receiveEventInfo(RequestInfo(request.httpRequest, response))
    }

  def addTag(text: String): Unit = {
    val now = nowMillis
    currentTags.add(TimedScenarioElement(now, now, TagElement(text)))
    frontEnd.receiveEventInfo(TagInfo(text))
  }

  def secureConnection(securedHostURI: Uri): Unit = {
    frontEnd.receiveEventInfo(SSLInfo(securedHostURI.toUrl))
  }

  def clearRecorderState(): Unit = {
    currentRequests.clear()
    currentTags.clear()
  }
}
