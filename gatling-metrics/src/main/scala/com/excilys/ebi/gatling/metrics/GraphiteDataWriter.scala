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
import com.excilys.ebi.gatling.metrics.types.{ RequestMetric, UserMetric }

case object SendToGraphite

class GraphiteDataWriter extends DataWriter {

	private var metricRootPath: List[String] = Nil
	private val allRequests = new RequestMetric
	private val perRequest: mutable.Map[String, RequestMetric] = new HashMap[String, RequestMetric]
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
			val metric = perRequest.getOrElseUpdate(requestName, new RequestMetric)
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
		case SendToGraphite => sendMetricsToGraphite
	}

	private def sendMetricsToGraphite {

		def sanitizeString(s: String) = s.replace(' ', '-')

		def sendToGraphite(metricPath: MetricPath, value: Long, epoch: Long) {

			writer.write(metricPath.toString)
			writer.write(" ")
			writer.write(value.toString)
			writer.write(" ")
			writer.write(epoch.toString)
			writer.write(END_OF_LINE)
		}

		def formatUserMetric(scenarioName: String, userMetric: UserMetric, epoch: Long) = {
			val rootPath = MetricPath("users", sanitizeString(scenarioName))
			sendToGraphite(rootPath + "active", userMetric.getActive, epoch)
			sendToGraphite(rootPath + "waiting", userMetric.getWaiting, epoch)
			sendToGraphite(rootPath + "done", userMetric.getDone, epoch)
		}

		def formatRequestMetric(requestName: String, requestMetric: RequestMetric, epoch: Long) = {
			val rootPath = MetricPath(sanitizeString(requestName))

			val (allCount, okCount, koCount) = requestMetric.counts
			sendToGraphite(rootPath + "all" + "count", allCount, epoch)
			sendToGraphite(rootPath + "ok" + "count", okCount, epoch)
			sendToGraphite(rootPath + "ko" + "count", koCount, epoch)

			val (allMax, okMax, koMax) = requestMetric.maxes
			if (allMax != 0)
				sendToGraphite(rootPath + "all" + "max", allMax, epoch)
			if (okMax != 0)
				sendToGraphite(rootPath + "ok" + "max", allMax, epoch)
			if (koMax != 0)
				sendToGraphite(rootPath + "ko" + "max", koMax, epoch)

			// FIXME percentile computation should use buckets
			sendToGraphite(rootPath + "all" + percentiles1Name, requestMetric.percentiles.all.getQuantile(percentiles1), epoch)
			sendToGraphite(rootPath + "all" + percentiles2Name, requestMetric.percentiles.all.getQuantile(percentiles2), epoch)
			sendToGraphite(rootPath + "ok" + percentiles1Name, requestMetric.percentiles.ok.getQuantile(percentiles1), epoch)
			sendToGraphite(rootPath + "ok" + percentiles2Name, requestMetric.percentiles.ok.getQuantile(percentiles2), epoch)
			sendToGraphite(rootPath + "ko" + percentiles1Name, requestMetric.percentiles.ko.getQuantile(percentiles1), epoch)
			sendToGraphite(rootPath + "ko" + percentiles2Name, requestMetric.percentiles.ko.getQuantile(percentiles2), epoch)
		}

		try {
			if (writer == null) writer = newWriter
			val epoch = nowMillis / 1000
			formatUserMetric("allUsers", allUsers, epoch)
			usersPerScenario.foreach {
				case (scenarioName, userMetric) => formatUserMetric(scenarioName, userMetric, epoch)
			}
			formatRequestMetric("allRequests", allRequests, epoch)
			perRequest.foreach {
				case (requestName, requestMetric) => formatRequestMetric(requestName, requestMetric, epoch)
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

