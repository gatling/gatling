/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import scala.annotation.tailrec
import scala.math.round
import scala.tools.nsc.io.{ Directory, File }

import org.codehaus.plexus.util.SelectorUtils
import org.jboss.netty.handler.codec.http.{ HttpMethod, HttpRequest, HttpResponse }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.PROXY_AUTHORIZATION

import com.excilys.ebi.gatling.http.ahc.GatlingAsyncHandlerActor.REDIRECT_STATUS_CODES
import com.excilys.ebi.gatling.recorder.config.{ Pattern, RecorderOptions }
import com.excilys.ebi.gatling.recorder.config.Configuration
import com.excilys.ebi.gatling.recorder.config.Configuration.configuration
import com.excilys.ebi.gatling.recorder.http.GatlingHttpProxy
import com.excilys.ebi.gatling.recorder.scenario.{ PauseElement, PauseUnit, RequestElement, ScenarioElement, ScenarioExporter, TagElement }
import com.excilys.ebi.gatling.recorder.ui.enumeration.{ FilterStrategy, PatternType }
import com.excilys.ebi.gatling.recorder.ui.frame.{ ConfigurationFrame, RunningFrame }
import com.excilys.ebi.gatling.recorder.ui.info.{ PauseInfo, RequestInfo, SSLInfo }
import com.excilys.ebi.gatling.recorder.ui.util.UIHelper.useUIThread
import com.ning.http.util.Base64

import grizzled.slf4j.Logging
import javax.swing.JOptionPane

object RecorderController {

	def apply(options: RecorderOptions) = {
		Configuration(options)
		val controller = new RecorderController
		controller.showConfigurationFrame
	}
}

class RecorderController extends Logging {
	private lazy val runningFrame: RunningFrame = new RunningFrame(this)
	private lazy val configurationFrame: ConfigurationFrame = new ConfigurationFrame(this)
	private val supportedHttpMethods = Vector(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD)

	@volatile private var startDate: Date = _
	@volatile private var lastRequestDate: Date = _
	@volatile private var lastRequest: HttpRequest = _
	@volatile private var lastStatus: Int = _
	@volatile private var proxy: GatlingHttpProxy = _
	@volatile private var scenarioElements: List[ScenarioElement] = Nil

	def startRecording {
		val response = if (File(ScenarioExporter.getOutputFolder / ScenarioExporter.getSimulationFileName(startDate)).exists)
			JOptionPane.showConfirmDialog(null, "You are about to overwrite an existing simulation.", "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
		else JOptionPane.OK_OPTION

		if (response == JOptionPane.OK_OPTION) {
			proxy = new GatlingHttpProxy(this, configuration.port, configuration.sslPort, configuration.proxy)
			startDate = new Date
			showRunningFrame
		}
	}

	def stopRecording {
		try {
			if (scenarioElements.isEmpty)
				info("Nothing was recorded, skipping scenario generation")
			else
				ScenarioExporter.saveScenario(startDate, scenarioElements.reverse)

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
					configuration.proxy.username = Some(credentials(0))
					configuration.proxy.password = Some(credentials(1))
			}
		}
	}

	def receiveResponse(request: HttpRequest, response: HttpResponse) {

		def processPause {
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
			} else
				lastRequestDate = new Date
		}

		def processRequest(request: HttpRequest, statusCode: Int) {

			// Store request in scenario elements
			scenarioElements = new RequestElement(request, statusCode, None) :: scenarioElements

			// Send request information to view
			useUIThread {
				runningFrame.receiveEventInfo(new RequestInfo(request, response))
			}
		}

		synchronized {
			if (isRequestAccepted(request)) {
				if (isRequestRedirectChainStart(request, response)) {
					processPause
					lastRequest = request

				} else if (isRequestRedirectChainEnd(request, response)) {
					// process request with new status
					processRequest(lastRequest, response.getStatus.getCode)
					lastRequest = null

				} else if (!isRequestInsideRedirectChain(request, response)) {
					// standard use case
					processPause
					processRequest(request, response.getStatus.getCode)
				}
			}

			lastStatus = response.getStatus.getCode
		}
	}

	def addTag(text: String) {
		scenarioElements = new TagElement(text) :: scenarioElements
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

	private def isRedirectCode(code: Int) = REDIRECT_STATUS_CODES.contains(code)

	private def isRequestRedirectChainStart(request: HttpRequest, response: HttpResponse): Boolean = configuration.followRedirect && !isRedirectCode(lastStatus) && isRedirectCode(response.getStatus.getCode)

	private def isRequestInsideRedirectChain(request: HttpRequest, response: HttpResponse): Boolean = configuration.followRedirect && isRedirectCode(lastStatus) && isRedirectCode(response.getStatus.getCode)

	private def isRequestRedirectChainEnd(request: HttpRequest, response: HttpResponse): Boolean = configuration.followRedirect && isRedirectCode(lastStatus) && !isRedirectCode(response.getStatus.getCode)

	private def isRequestAccepted(request: HttpRequest): Boolean = {

		def requestMatched = {
			val path = new URI(request.getUri).getPath

			def gatlingPatternToPlexusPattern(pattern: Pattern) = {

				val prefix = pattern.patternType match {
					case PatternType.ANT => SelectorUtils.ANT_HANDLER_PREFIX
					case PatternType.JAVA => SelectorUtils.REGEX_HANDLER_PREFIX
				}

				prefix + pattern.getPattern + SelectorUtils.PATTERN_HANDLER_SUFFIX
			}

			@tailrec
			def matchPath(patterns: List[Pattern]): Boolean = patterns match {
				case Nil => false
				case head :: tail =>
					if (SelectorUtils.matchPath(gatlingPatternToPlexusPattern(head), path)) true
					else matchPath(tail)
			}

			matchPath(configuration.patterns)
		}

		def requestPassFilters = configuration.filterStrategy match {
			case FilterStrategy.EXCEPT => !requestMatched
			case FilterStrategy.ONLY => requestMatched
			case FilterStrategy.NONE => true
		}

		supportedHttpMethods.contains(request.getMethod) && requestPassFilters
	}

	private def getFolder(folderName: String, folderPath: String): Directory = Directory(folderPath).createDirectory()

	private def getOutputFolder = getFolder("output", configuration.outputFolder)
}