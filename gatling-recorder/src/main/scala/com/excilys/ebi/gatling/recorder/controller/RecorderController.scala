/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.recorder.controller

import java.net.URI
import java.util.Date

import scala.math.round
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Directory

import org.codehaus.plexus.util.SelectorUtils
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.PROXY_AUTHORIZATION
import org.jboss.netty.handler.codec.http.{ HttpResponse, HttpRequest, HttpMethod }

import com.excilys.ebi.gatling.recorder.config.Configuration.configuration
import com.excilys.ebi.gatling.recorder.config.{ Options, Configuration }
import com.excilys.ebi.gatling.recorder.http.GatlingHttpProxy
import com.excilys.ebi.gatling.recorder.scenario.{ ScenarioExporter, ScenarioElement, RequestElement, PauseUnit, PauseElement }
import com.excilys.ebi.gatling.recorder.ui.enumeration.{ PatternType, FilterStrategy }
import com.excilys.ebi.gatling.recorder.ui.frame.{ RunningFrame, ConfigurationFrame }
import com.excilys.ebi.gatling.recorder.ui.info.{ SSLInfo, RequestInfo, PauseInfo }
import com.excilys.ebi.gatling.recorder.ui.util.UIHelper.useUIThread
import com.ning.http.util.Base64

import grizzled.slf4j.Logging

object RecorderController extends Logging {
	private val runningFrame: RunningFrame = new RunningFrame
	private val configurationFrame: ConfigurationFrame = new ConfigurationFrame
	private val supportedHttpMethods = Vector(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD)

	@volatile private var startDate: Date = _
	@volatile private var lastRequestDate: Date = _

	@volatile private var scenarioElements = List[ScenarioElement]()

	def apply(options: Options) {
		Configuration(options)
		configurationFrame.populateItemsFromConfiguration(configuration)
		showConfigurationFrame
	}

	def startRecording {
		GatlingHttpProxy(configuration.port, configuration.sslPort, configuration.proxy)

		startDate = new Date

		showRunningFrame
	}

	def stopRecording {
		GatlingHttpProxy.shutdown

		// Save Scenario to Disk only if there are recorded elements
		if (!scenarioElements.isEmpty)
			ScenarioExporter.saveScenario(startDate, scenarioElements.reverse)

		clearRecorderState

		showConfigurationFrame
	}

	def receiveRequest(request: HttpRequest) {
		synchronized {
			// If Outgoing Proxy set, we record the credentials to use them when sending the request
			Option(request.getHeader(PROXY_AUTHORIZATION)).map { header =>
				// Split on " " and take 2nd group (Basic credentialsInBase64==)
				val credentials = new String(Base64.decode(header.split(" ")(1))).split(":")
				configuration.proxy.username = Some(credentials(0))
				configuration.proxy.password = Some(credentials(1))
			}
		}
	}

	def receiveResponse(request: HttpRequest, response: HttpResponse) {
		synchronized {
			if (isRequestToBeAdded(request)) {
				processRequest(request, response)

				// Pause calculation
				if (lastRequestDate != null) {
					val newRequestDate = new Date
					val diff = newRequestDate.getTime - lastRequestDate.getTime
					if (diff > 10) {

						val (pauseValue, pauseUnit) =
							if (diff > 1000)
								(round(diff / 1000).toLong, PauseUnit.SECONDS)
							else
								(diff, PauseUnit.MILLISECONDS)

						lastRequestDate = newRequestDate
						useUIThread {
							runningFrame.receiveEventInfo(new PauseInfo(pauseValue, pauseUnit))
						}

						scenarioElements = new PauseElement(pauseValue, pauseUnit) :: scenarioElements
					}
				} else {
					lastRequestDate = new Date
				}
			}
		}
	}

	def secureConnection(securedHostURI: URI) {
		useUIThread {
			runningFrame.receiveEventInfo(SSLInfo(securedHostURI.toString))
		}
	}

	private def showRunningFrame {
		runningFrame.setVisible(true)
		configurationFrame.setVisible(false)
	}

	private def showConfigurationFrame {
		configurationFrame.setVisible(true)
		runningFrame.setVisible(false)
	}

	def clearRecorderState {
		runningFrame.clearState

		scenarioElements = Nil
		startDate = new Date
		lastRequestDate = null
	}

	private def isRequestToBeAdded(request: HttpRequest): Boolean = {
		val uri = new URI(request.getUri)
		if (supportedHttpMethods.contains(request.getMethod)) {
			if (configuration.filterStrategy != FilterStrategy.NONE) {

				val uriMatched = (for (configPattern <- configuration.patterns) yield {
					val pattern = configPattern.patternType match {
						case PatternType.ANT => SelectorUtils.ANT_HANDLER_PREFIX
						case PatternType.JAVA => SelectorUtils.REGEX_HANDLER_PREFIX
					}

					SelectorUtils.matchPath(pattern + configPattern.pattern + SelectorUtils.PATTERN_HANDLER_SUFFIX, uri.getPath)
				}).foldLeft(false)(_ || _)

				if (configuration.filterStrategy == FilterStrategy.ONLY)
					uriMatched
				else
					!uriMatched
			} else
				true
		} else
			false
	}

	private def getFolder(folderName: String, folderPath: String): Directory = Directory(folderPath).createDirectory()

	private def getOutputFolder = getFolder("output", configuration.outputFolder)

	private def processRequest(request: HttpRequest, response: HttpResponse) {

		// Store request in scenario elements
		scenarioElements = new RequestElement(request, response.getStatus.getCode, None) :: scenarioElements

		// Send request information to view
		useUIThread {
			runningFrame.receiveEventInfo(new RequestInfo(request, response))
		}
	}
}