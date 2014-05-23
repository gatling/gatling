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
package io.gatling.recorder.controller

import java.net.URI
import java.net.URL

import scala.collection.mutable
import scala.concurrent.duration.DurationLong
import scala.reflect.io.Path.string2path
import scala.tools.nsc.io.File
import org.jboss.netty.handler.codec.http.{ HttpRequest, HttpResponse }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.PROXY_AUTHORIZATION
import com.ning.http.util.Base64
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.validation.{ Failure, Success }
import io.gatling.recorder.{ Har, Proxy }
import io.gatling.recorder.config.{ RecorderConfiguration, RecorderPropertiesBuilder }
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.model.{ SimulationModel, RequestModel }
import io.gatling.recorder.ui.{ PauseInfo, RecorderFrontend, RequestInfo, SSLInfo, TagInfo }
import io.gatling.recorder.export.{ Exporter, HarExporter }

object RecorderController {
  def apply(props: Map[String, Any], recorderConfigFile: Option[File] = None) {
    RecorderConfiguration.initialSetup(props, recorderConfigFile)
    new RecorderController
  }
}

class RecorderController extends StrictLogging {

  import scala.collection.mutable.{ Map, SynchronizedMap, HashMap }

  private val frontEnd = RecorderFrontend.newFrontend(this)
  @volatile private var proxy: HttpProxy = _
  implicit var model: SimulationModel = _
  // state tracking across request arrival/completion events
  val arrivalsMap = MapMaker.makeMap
  // used for calculating pauses
  var previousCompletionTime: Long = 0

  frontEnd.init()

  // TODO - har does not "record" so refactor this
  def startRecording() {
    val selectedMode = frontEnd.selectedMode
    if (selectedMode == Har && !File(frontEnd.harFilePath).exists) {
      frontEnd.handleMissingHarFile(frontEnd.harFilePath)
    } else {
      implicit val config = configuration
      model = new SimulationModel()
      val proceed = if (Exporter.simulationExists) frontEnd.askSimulationOverwrite else true
      if (proceed) {
        selectedMode match {
          case Har => {
            val exporter = new HarExporter(frontEnd.harFilePath)
            exporter.exportHar match {
              case Failure(errMsg) => frontEnd.handleHarExportFailure(errMsg)
              case _               => frontEnd.handleHarExportSuccess()
            }
            // har finished here.
          }
          case Proxy =>
            proxy = new HttpProxy(config, this)
            frontEnd.recordingStarted()
        }
      }
    }
  }

  // only applies to proxy
  def stopRecording(save: Boolean) {

    try {
      frontEnd.recordingStopped()
      if (model.isEmpty)
        logger.info("Nothing was recorded, skipping simulation generation")
      else {
        implicit val config = configuration

        val exporter = new Exporter()
        model.postProcess
        exporter.export(model) match {
          case Failure(errMsg) => // TODO // frontEnd.handleExportFailure(errMsg)
          case _               => // TODO // frontEnd.handleExportSuccess()
        }
      }

    } finally {
      proxy.shutdown()
      clearRecorderState()
      frontEnd.init()
    }
  }

  // proxy receives a request
  def receiveRequest(request: HttpRequest) {

    val arrivalTime = System.currentTimeMillis
    // we'll hoik the arrival time out once the request is completed...
    arrivalsMap += request -> arrivalTime
    model.setProxyAuth(extractProxyAuth(request))
  }

  // proxy receives a response
  def receiveResponse(request: HttpRequest, response: HttpResponse) {

    val notFiltered = configuration.filters.filters.map(_.accept(request.getUri)).getOrElse(true)
    if (notFiltered) {

      // we want to order the requests on their arrival time, not completion time
      val arrivalTime = arrivalsMap(request) // System.currentTimeMillis
      val requestEl = RequestModel(request, response)

      // Notify the model + FE of new Pause
      val delta = (arrivalTime - previousCompletionTime).milliseconds
      val thereWasAPause = previousCompletionTime > 0 && delta > configuration.core.thresholdForPauseCreation
      if (thereWasAPause) {
        frontEnd.receiveEventInfo(PauseInfo(delta))
        model addPause (delta)
      }

      //notify the model + FE of new Request
      model += (arrivalTime, requestEl)
      frontEnd.receiveEventInfo(RequestInfo(request, response))
    }

    previousCompletionTime = System.currentTimeMillis
  }

  // A tag is the boundary between Navigations in the model
  def addTag(text: String) {

    model newNavigation (System.currentTimeMillis, text)
    frontEnd.receiveEventInfo(TagInfo(text))
  }

  //TODO - document what this is
  def secureConnection(securedHostURI: URI) {
    frontEnd.receiveEventInfo(SSLInfo(securedHostURI.toString))
  }

  def clearRecorderState() {
    implicit val config = configuration
    model = new SimulationModel()
    arrivalsMap.clear
    // don't produce a long pause at the top once re-recording...
    previousCompletionTime = 0
  }

  object MapMaker {
    def makeMap: Map[HttpRequest, Long] = {
      new HashMap[HttpRequest, Long] with SynchronizedMap[HttpRequest, Long] {
        override def default(key: HttpRequest) =
          -1
      }
    }
  }

  private def extractProxyAuth(request: HttpRequest): Option[(String, String)] = {

    Option(request.headers.get(PROXY_AUTHORIZATION)).map {
      header =>
        // Split on " " and take 2nd group (Basic credentialsInBase64==)
        val credentials = new String(Base64.decode(header.split(" ")(1))).split(":")

        (credentials(0), credentials(1)) // usename password
    }
  }

}