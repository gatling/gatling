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
package com.excilys.ebi.gatling.metrics

import java.io.{ BufferedWriter, IOException, OutputStreamWriter, Writer }
import java.net.Socket
import java.util.{ HashMap, Timer, TimerTask }
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.{ RequestRecord, RunRecord, ShortScenarioDescription }
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis
import com.excilys.ebi.gatling.metrics.types.{ RequestMetrics, UserMetric, Metrics }

case object SendToGraphite

class GraphiteDataWriter extends DataWriter {

	private var metricRootPath: List[String] = Nil
	private val allRequests = new RequestMetrics
	private val perRequest: mutable.Map[String, RequestMetrics] = new HashMap[String, RequestMetrics]
	private var allUsers: UserMetric = _
	private val usersPerScenario: mutable.Map[String, UserMetric] = new HashMap[String, UserMetric]
	private var timer: Timer = _
	private var writer: Writer = _
	private val percentiles1 = configuration.charting.indicators.percentile1
	private val percentiles1Name = "percentiles" + percentiles1
	private val percentiles2 = configuration.charting.indicators.percentile2
	private val percentiles2Name = "percentiles" + percentiles2

	private def newWriter(): Writer = {
		val socket = new Socket(configuration.graphite.host, configuration.graphite.port)
		new BufferedWriter(new OutputStreamWriter(socket.getOutputStream))
	}

	def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) {
		metricRootPath = List("gatling", runRecord.simulationId)
		allUsers = new UserMetric(scenarios.map(_.nbUsers).sum)
		scenarios.foreach(scenario => usersPerScenario.+=((scenario.name, new UserMetric(scenario.nbUsers))))
		writer = newWriter
		timer = new Timer(true)
		timer.scheduleAtFixedRate(new SendToGraphiteTask, 0, 1000)
	}

	def onRequestRecord(requestRecord: RequestRecord) {
		//Update request metrics
		val requestName = requestRecord.requestName
		if (requestName != START_OF_SCENARIO && requestName != END_OF_SCENARIO) {
			val metric = perRequest.getOrElseUpdate(requestName, new RequestMetrics)
			metric.update(requestRecord)
		}
		allRequests.update(requestRecord)

		// Update sessions metrics
		usersPerScenario(requestRecord.scenarioName).update(requestRecord)
		allUsers.update(requestRecord)
	}

	def onFlushDataWriter {
		writer.close
	}

	override def receive = uninitialized

	override def initialized: Receive = super.initialized.orElse {
		case SendToGraphite => sendMetricsToGraphite(nowMillis / 1000)
	}

	private def sendMetricsToGraphite(epoch: Long) {

		def sanitizeString(s: String) = s.replace(' ', '_').replace('.', '-').replace('\\', '-')

		def sendToGraphite(metricPath: MetricPath, value: Long) {
			writer.write(metricPath.toString)
			writer.write(" ")
			writer.write(value.toString)
			writer.write(" ")
			writer.write(epoch.toString)
			writer.write(END_OF_LINE)
		}

		def sendUserMetrics(scenarioName: String, userMetric: UserMetric) = {
			val rootPath = MetricPath("users", sanitizeString(scenarioName))
			sendToGraphite(rootPath + "active", userMetric.getActive)
			sendToGraphite(rootPath + "waiting", userMetric.getWaiting)
			sendToGraphite(rootPath + "done", userMetric.getDone)
		}

		def sendMetrics(metricPath: MetricPath, metrics: Metrics) = {

			sendToGraphite(metricPath + "count", metrics.count)
			if (metrics.count > 0) {
				sendToGraphite(metricPath + "min", metrics.min)
				sendToGraphite(metricPath + "max", metrics.max)
				sendToGraphite(metricPath + percentiles1Name, metrics.sample.getQuantile(percentiles1))
				sendToGraphite(metricPath + percentiles2Name, metrics.sample.getQuantile(percentiles2))
			}
		}

		def sendRequestMetrics(requestName: String, requestMetrics: RequestMetrics) = {
			val rootPath = MetricPath(sanitizeString(requestName))

			val (okMetrics, koMetrics, allMetrics) = requestMetrics.metrics

			sendMetrics(rootPath + "ok", okMetrics)
			sendMetrics(rootPath + "ko", koMetrics)
			sendMetrics(rootPath + "all", allMetrics)

			requestMetrics.reset
		}

		try {
			if (writer == null) writer = newWriter

			sendUserMetrics("allUsers", allUsers)
			usersPerScenario.foreach {
				case (scenarioName, userMetric) => sendUserMetrics(scenarioName, userMetric)
			}
			sendRequestMetrics("allRequests", allRequests)
			perRequest.foreach {
				case (requestName, requestMetric) => sendRequestMetrics(requestName, requestMetric)
			}

			writer.flush

		} catch {
			case e: IOException => {
				error("Error writing to Graphite", e)
				writer.close
				writer = null
			}
		}
	}

	private class SendToGraphiteTask extends TimerTask {
		def run {
			self ! SendToGraphite
		}
	}

	private object MetricPath {

		def apply(elements: String*) = new MetricPath(metricRootPath ::: elements.toList)
	}

	private class MetricPath(path: List[String]) {

		def +(element: String) = new MetricPath(path ::: List(element))

		override def toString = path.mkString(".")
	}
}

