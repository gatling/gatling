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

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.DurationLong
import scala.tools.nsc.io.File

import org.jboss.netty.handler.codec.http.{ HttpRequest, HttpResponse }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.PROXY_AUTHORIZATION

import com.ning.http.util.Base64
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.recorder.{ Har, Proxy }
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.config.RecorderPropertiesBuilder
import io.gatling.recorder.http.GatlingHttpProxy
import io.gatling.recorder.scenario.{ PauseElement, RequestElement, ScenarioElement, ScenarioExporter, TagElement }
import io.gatling.recorder.ui._
import io.gatling.recorder.util.FiltersHelper.isRequestAccepted
import io.gatling.recorder.util.RedirectHelper._
import io.gatling.recorder.har.HarReader

object RecorderController {

	def apply(props: mutable.Map[String, Any], recorderConfigFile: Option[File] = None) {
		RecorderConfiguration.initialSetup(props, recorderConfigFile)
		new RecorderController
	}
}

class RecorderController extends Logging {

	private val frontEnd = RecorderFrontend.newFrontend(this)
	@volatile private var lastRequestTimestamp: Long = 0
	@volatile private var redirectChainStart: HttpRequest = _
	@volatile private var proxy: GatlingHttpProxy = _
	@volatile private var scenarioElements: List[ScenarioElement] = Nil

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
							HarReader.processHarFile(harFilePath)
							ScenarioExporter.saveScenario(HarReader.scenarioElements.reverse)
							frontEnd.handleHarExportSuccess
						} catch {
							case e: Exception =>
								logger.error("Error while processing HAR file", e)
								frontEnd.handleHarExportFailure
						} finally {
							HarReader.cleanHarReaderState
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
			if (scenarioElements.isEmpty)
				logger.info("Nothing was recorded, skipping scenario generation")
			else
				ScenarioExporter.saveScenario(scenarioElements.reverse)

			proxy.shutdown

		} finally {
			clearRecorderState
			frontEnd.init
		}
	}

	def receiveRequest(request: HttpRequest) {
		synchronized {
			// If Outgoing Proxy set, we record the credentials to use them when sending the request
			Option(request.getHeader(PROXY_AUTHORIZATION)).map {
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

		def processPause {
			// Pause calculation
			if (lastRequestTimestamp != 0) {
				val newRequestTimestamp = System.currentTimeMillis
				val diff = newRequestTimestamp - lastRequestTimestamp
				if (diff > 10) {
					val pauseDuration = diff.milliseconds
					lastRequestTimestamp = newRequestTimestamp
					frontEnd.receiveEventInfo(PauseInfo(pauseDuration))

					@tailrec
					def insertPauseAfterTags(elements: List[ScenarioElement], tags: List[TagElement]): List[ScenarioElement] = elements match {
						case Nil => (new PauseElement(pauseDuration) :: tags).reverse
						case (t: TagElement) :: others => insertPauseAfterTags(others, t :: tags)
						case _ => tags reverse_::: new PauseElement(pauseDuration) :: elements
					}
					scenarioElements = insertPauseAfterTags(scenarioElements, Nil)
				}
			} else
				lastRequestTimestamp = System.currentTimeMillis
		}

		def processRequest(request: HttpRequest, statusCode: Int) {

			// Store request in scenario elements
			scenarioElements = RequestElement(request, statusCode, None) :: scenarioElements

			// Send request information to view
			frontEnd.receiveEventInfo(RequestInfo(request, response))
		}

		synchronized {
			if (isRequestAccepted(request.getUri, request.getMethod.toString)) {
				if (redirectChainStart == null && isRequestRedirect(response.getStatus.getCode)) {
					// enter redirect chain
					processPause
					redirectChainStart = request

				} else if (redirectChainStart != null && !isRequestRedirect(response.getStatus.getCode)) {
					// exit redirect chain
					// process request with new status
					processRequest(redirectChainStart, response.getStatus.getCode)
					redirectChainStart = null

				} else if (redirectChainStart == null) {
					// standard use case
					processPause
					processRequest(request, response.getStatus.getCode)
				}
			}
		}
	}

	def addTag(text: String) {
		scenarioElements = new TagElement(text) :: scenarioElements
		frontEnd.receiveEventInfo(TagInfo(text))
	}

	def secureConnection(securedHostURI: URI) {
		frontEnd.receiveEventInfo(SSLInfo(securedHostURI.toString))
	}

	def clearRecorderState {
		scenarioElements = Nil
		lastRequestTimestamp = 0
	}

}