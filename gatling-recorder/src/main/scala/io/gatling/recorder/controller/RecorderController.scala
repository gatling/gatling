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
import scala.tools.nsc.io.File
import scala.swing.Swing.onEDT

import org.jboss.netty.handler.codec.http.{ HttpRequest, HttpResponse }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.PROXY_AUTHORIZATION

import com.ning.http.util.Base64
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.config.RecorderPropertiesBuilder
import io.gatling.recorder.http.GatlingHttpProxy
import io.gatling.recorder.scenario.{ PauseElement, RequestElement, ScenarioElement, ScenarioExporter, TagElement }
import io.gatling.recorder.ui.frame.{ ConfigurationFrame, RunningFrame }
import io.gatling.recorder.ui.frame.ConfigurationFrame.{ HarMode, HttpMode }
import io.gatling.recorder.ui.info.{ PauseInfo, RequestInfo, SSLInfo, TagInfo }
import io.gatling.recorder.util.FiltersHelper.isRequestAccepted
import io.gatling.recorder.util.RedirectHelper._
import javax.swing.JOptionPane
import io.gatling.recorder.har.HarReader

object RecorderController {

	def apply(props: mutable.Map[String, Any], recorderConfigFile: Option[File] = None) {
		RecorderConfiguration.initialSetup(props, recorderConfigFile)
		val controller = new RecorderController
		controller.showConfigurationFrame
	}
}

class RecorderController extends Logging {
	private lazy val runningFrame = new RunningFrame(this)
	private lazy val configurationFrame = new ConfigurationFrame(this)

	@volatile private var lastRequestTimestamp: Long = 0
	@volatile private var redirectChainStart: HttpRequest = _
	@volatile private var proxy: GatlingHttpProxy = _
	@volatile private var scenarioElements: List[ScenarioElement] = Nil

	def startRecording {
		val selectedMode = configurationFrame.modeSelector.selection.item
		val harFilePath = configurationFrame.harFilePath.text
		if (selectedMode == HarMode && harFilePath.isEmpty) {
			JOptionPane.showMessageDialog(null, "You haven't selected an HAR file.", "Error", JOptionPane.ERROR_MESSAGE)
		} else {
			val response = if (File(ScenarioExporter.getOutputFolder / ScenarioExporter.getSimulationFileName).exists)
				JOptionPane.showConfirmDialog(null, "You are about to overwrite an existing simulation.", "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
			else JOptionPane.OK_OPTION

			if (response == JOptionPane.OK_OPTION) {
				selectedMode match {
					case HarMode =>
						HarReader.processHarFile(harFilePath)
						ScenarioExporter.saveScenario(HarReader.scenarioElements.reverse)
						JOptionPane.showMessageDialog(null, "Successfully converted HAR file to a Gatling simulation", "Conversion complete", JOptionPane.INFORMATION_MESSAGE)
						HarReader.cleanHarReaderState
					case HttpMode =>
						proxy = new GatlingHttpProxy(this, configuration.proxy.port, configuration.proxy.sslPort)
						showRunningFrame
				}
			}
		}
	}

	def stopRecording {
		try {
			if (scenarioElements.isEmpty)
				logger.info("Nothing was recorded, skipping scenario generation")
			else
				ScenarioExporter.saveScenario(scenarioElements.reverse)

			proxy.shutdown

		} finally {
			clearRecorderState
			showConfigurationFrame
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
					onEDT {
						runningFrame.receiveEventInfo(PauseInfo(pauseDuration))
					}

					scenarioElements = new PauseElement(pauseDuration) :: scenarioElements
				}
			} else
				lastRequestTimestamp = System.currentTimeMillis
		}

		def processRequest(request: HttpRequest, statusCode: Int) {

			// Store request in scenario elements
			scenarioElements = RequestElement(request, statusCode, None) :: scenarioElements

			// Send request information to view
			onEDT {
				runningFrame.receiveEventInfo(RequestInfo(request, response))
			}
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
		runningFrame.receiveEventInfo(TagInfo(text))
	}

	def secureConnection(securedHostURI: URI) {
		onEDT {
			runningFrame.receiveEventInfo(SSLInfo(securedHostURI.toString))
		}
	}

	private def showRunningFrame {
		runningFrame.visible = true
		configurationFrame.visible = false
	}

	private def showConfigurationFrame {
		configurationFrame.visible = true
		runningFrame.visible = false
	}

	def clearRecorderState {
		runningFrame.clearState

		scenarioElements = Nil
		lastRequestTimestamp = 0
	}

}