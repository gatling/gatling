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
package io.gatling.recorder.controller

import java.net.URI

import scala.collection.mutable
import scala.concurrent.duration.DurationLong
import scala.reflect.io.Path.string2path
import scala.tools.nsc.io.File

import org.jboss.netty.handler.codec.http.{ HttpRequest, HttpResponse }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.PROXY_AUTHORIZATION

import com.ning.http.util.Base64
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.recorder.{ Har, Proxy }
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.config.RecorderPropertiesBuilder
import io.gatling.recorder.har.HarReader
import io.gatling.recorder.http.GatlingHttpProxy
import io.gatling.recorder.scenario.{ RequestElement, Scenario, ScenarioExporter, TagElement }
import io.gatling.recorder.ui.{ PauseInfo, RecorderFrontend, RequestInfo, SSLInfo, TagInfo }

object RecorderController {

	def apply(props: mutable.Map[String, Any], recorderConfigFile: Option[File] = None) {
		RecorderConfiguration.initialSetup(props, recorderConfigFile)
		new RecorderController
	}
}

class RecorderController extends Logging {

	private val frontEnd = RecorderFrontend.newFrontend(this)

	@volatile private var proxy: GatlingHttpProxy = _

	// Can use ConcurrentLinkedDeque when dropping support of JDK6
	// Collection of tuples, (arrivalTime, request)
	private val currentRequests = new mutable.ArrayBuffer[(Long, RequestElement)] with mutable.SynchronizedBuffer[(Long, RequestElement)]
	// Collection of tuples, (arrivalTime, tag)
	private val currentTags = new mutable.ArrayBuffer[(Long, TagElement)] with mutable.SynchronizedBuffer[(Long, TagElement)]

	frontEnd.init

	def startRecording {
		val selectedMode = frontEnd.selectedMode
		val harFilePath = frontEnd.harFilePath
		if (selectedMode == Har && !File(harFilePath).exists) {
			frontEnd.handleMissingHarFile(harFilePath)
		} else {
			val simulationFile = File(ScenarioExporter.getOutputFolder / ScenarioExporter.getSimulationFileName)
			val proceed = if (simulationFile.exists) frontEnd.askSimulationOverwrite else true
			if (proceed) {
				selectedMode match {
					case Har =>
						try {
							ScenarioExporter.saveScenario(HarReader(harFilePath))
							frontEnd.handleHarExportSuccess
						} catch {
							case e: Exception =>
								logger.error("Error while processing HAR file", e)
								frontEnd.handleHarExportFailure
						}
					case Proxy =>
						proxy = new GatlingHttpProxy(this, configuration.proxy.port, configuration.proxy.sslPort)
						frontEnd.recordingStarted
				}
			}
		}
	}

	def stopRecording(save: Boolean) {
		frontEnd.recordingStopped
		try {
			if (currentRequests.isEmpty)
				logger.info("Nothing was recorded, skipping scenario generation")
			else {
				val scenario = Scenario(currentRequests.toVector, currentTags.toVector)
				ScenarioExporter.saveScenario(scenario)
			}

			proxy.shutdown

		} finally {
			clearRecorderState
			frontEnd.init
		}
	}

	def receiveRequest(request: HttpRequest) {
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
	}

	def receiveResponse(request: HttpRequest, response: HttpResponse) {
		if (configuration.filters.filters.map(_.accept(request.getUri)).getOrElse(true)) {
			val arrivalTime = System.currentTimeMillis

			val previousArrivalTime = currentRequests.lastOption.map(_._1)

			val requestEl = RequestElement(request, response.getStatus.getCode)
			currentRequests += arrivalTime -> requestEl

			// Notify the frontend
			previousArrivalTime.foreach { t =>
				val delta = (arrivalTime - t).milliseconds
				if (delta > configuration.core.thresholdForPauseCreation)
					frontEnd.receiveEventInfo(PauseInfo(delta))
			}
			frontEnd.receiveEventInfo(RequestInfo(request, response))
		}
	}

	def addTag(text: String) {
		currentTags += System.currentTimeMillis -> TagElement(text)
		frontEnd.receiveEventInfo(TagInfo(text))
	}

	def secureConnection(securedHostURI: URI) {
		frontEnd.receiveEventInfo(SSLInfo(securedHostURI.toString))
	}

	def clearRecorderState {
		currentRequests.clear
		currentTags.clear
	}

}