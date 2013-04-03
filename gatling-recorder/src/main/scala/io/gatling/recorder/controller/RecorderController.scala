/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
import java.util.Date

import scala.annotation.tailrec
import scala.collection.mutable
import scala.math.round
import scala.tools.nsc.io.File

import org.codehaus.plexus.util.SelectorUtils
import org.jboss.netty.handler.codec.http.{ HttpMethod, HttpRequest, HttpResponse }
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.PROXY_AUTHORIZATION

import com.ning.http.util.Base64
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.http.ahc.GatlingAsyncHandlerActor.redirectStatusCodes
import io.gatling.recorder.config.{ Pattern, RecorderConfiguration }
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.config.RecorderPropertiesBuilder
import io.gatling.recorder.http.GatlingHttpProxy
import io.gatling.recorder.scenario.{ PauseElement, PauseUnit, RequestElement, ScenarioElement, ScenarioExporter, TagElement }
import io.gatling.recorder.ui.enumeration.{ FilterStrategy, PatternType }
import io.gatling.recorder.ui.frame.{ ConfigurationFrame, RunningFrame }
import io.gatling.recorder.ui.info.{ PauseInfo, RequestInfo, SSLInfo }
import io.gatling.recorder.ui.util.UIHelper.useUIThread
import javax.swing.JOptionPane

object RecorderController {

	def apply(props: mutable.Map[String, Any]) {
		RecorderConfiguration.initialSetup(props)
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
			proxy = new GatlingHttpProxy(this, configuration.proxy.port, configuration.proxy.sslPort)
			startDate = new Date
			showRunningFrame
		}
	}

	def stopRecording {
		try {
			if (scenarioElements.isEmpty)
				logger.info("Nothing was recorded, skipping scenario generation")
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

	private def isRedirectCode(code: Int) = redirectStatusCodes.contains(code)

	private def isRequestRedirectChainStart(request: HttpRequest, response: HttpResponse): Boolean = configuration.http.followRedirect && !isRedirectCode(lastStatus) && isRedirectCode(response.getStatus.getCode)

	private def isRequestInsideRedirectChain(request: HttpRequest, response: HttpResponse): Boolean = configuration.http.followRedirect && isRedirectCode(lastStatus) && isRedirectCode(response.getStatus.getCode)

	private def isRequestRedirectChainEnd(request: HttpRequest, response: HttpResponse): Boolean = configuration.http.followRedirect && isRedirectCode(lastStatus) && !isRedirectCode(response.getStatus.getCode)

	private def isRequestAccepted(request: HttpRequest): Boolean = {

		def requestMatched = {
			val path = new URI(request.getUri).getPath

			def gatlingPatternToPlexusPattern(pattern: Pattern) = {

				val prefix = pattern.patternType match {
					case PatternType.ANT => SelectorUtils.ANT_HANDLER_PREFIX
					case PatternType.JAVA => SelectorUtils.REGEX_HANDLER_PREFIX
				}

				prefix + pattern.pattern + SelectorUtils.PATTERN_HANDLER_SUFFIX
			}

			@tailrec
			def matchPath(patterns: List[Pattern]): Boolean = patterns match {
				case Nil => false
				case head :: tail =>
					if (SelectorUtils.matchPath(gatlingPatternToPlexusPattern(head), path)) true
					else matchPath(tail)
			}

			matchPath(configuration.filters.patterns)
		}

		def requestPassFilters = configuration.filters.filterStrategy match {
			case FilterStrategy.EXCEPT => !requestMatched
			case FilterStrategy.ONLY => requestMatched
			case FilterStrategy.NONE => true
		}

		supportedHttpMethods.contains(request.getMethod) && requestPassFilters
	}
}