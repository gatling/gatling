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
package com.excilys.ebi.gatling.metrics

import java.io.IOException
import java.net.{ DatagramPacket, DatagramSocket, InetSocketAddress }
import java.nio.channels.DatagramChannel
import java.util.{ Timer, TimerTask }

import scala.collection.mutable

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.Group
import com.excilys.ebi.gatling.core.result.message.{ GroupRecord, RequestRecord, RunRecord, ScenarioRecord, ShortScenarioDescription }
import com.excilys.ebi.gatling.core.result.message.RecordEvent.{ END, START }
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.core.util.TimeHelper.nowSeconds
import com.excilys.ebi.gatling.metrics.types.{ Metrics, RequestMetrics, UserMetric }

case object SendToGraphite

class GraphiteDataWriter extends DataWriter {

	private var metricRootPath: List[String] = Nil
	private val groupStack: mutable.Map[(String, Int), Option[Group]] = mutable.HashMap.empty
	private val allRequests = new RequestMetrics
	private val perRequest: mutable.Map[List[String], RequestMetrics] = mutable.HashMap.empty
	private var allUsers: UserMetric = _
	private val usersPerScenario: mutable.Map[String, UserMetric] = mutable.HashMap.empty
	private var timer: Timer = _
	private var socket: DatagramSocket = _
	private val address = new InetSocketAddress(configuration.graphite.host, configuration.graphite.port)
	private val percentiles1 = configuration.charting.indicators.percentile1
	private val percentiles1Name = "percentiles" + percentiles1
	private val percentiles2 = configuration.charting.indicators.percentile2
	private val percentiles2Name = "percentiles" + percentiles2

	private def newSocket = DatagramChannel.open.socket

	def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) {
		metricRootPath = List("gatling", runRecord.simulationId)
		allUsers = new UserMetric(scenarios.map(_.nbUsers).sum)
		scenarios.foreach(scenario => usersPerScenario.+=((scenario.name, new UserMetric(scenario.nbUsers))))
		socket = newSocket
		timer = new Timer(true)
		timer.scheduleAtFixedRate(new SendToGraphiteTask, 0, 1000)
	}

	def onScenarioRecord(scenarioRecord: ScenarioRecord) {
		usersPerScenario(scenarioRecord.scenarioName).update(scenarioRecord)
		allUsers.update(scenarioRecord)
		scenarioRecord.event match {
			case START => updateCurrentGroup(scenarioRecord.scenarioName, scenarioRecord.userId, _ => None)
			case END => groupStack.remove((scenarioRecord.scenarioName, scenarioRecord.userId))
		}
	}

	private def updateCurrentGroup(scenarioName: String, userId: Int, value: Option[Group] => Option[Group]) =
		groupStack += (scenarioName, userId) -> value(groupStack.get(scenarioName -> userId).flatten)

	def onGroupRecord(groupRecord: GroupRecord) {
		groupRecord.event match {
			case START =>
				updateCurrentGroup(groupRecord.scenarioName, groupRecord.userId, current => Some(Group(groupRecord.groupName, current)))

			case END =>
				updateCurrentGroup(groupRecord.scenarioName, groupRecord.userId, current => current.flatMap(_.parent))
		}
	}

	def onRequestRecord(requestRecord: RequestRecord) {
		val group = groupStack(requestRecord.scenarioName -> requestRecord.userId)
		val path = requestRecord.requestName :: group.map(group => group.name :: group.groups).getOrElse(Nil)
		val metric = perRequest.getOrElseUpdate(path.reverse, new RequestMetrics)
		metric.update(requestRecord)
		allRequests.update(requestRecord)
	}

	def onFlushDataWriter {
		socket.close
	}

	override def receive = uninitialized

	override def initialized: Receive = super.initialized.orElse {
		case SendToGraphite => sendMetricsToGraphite(nowSeconds)
	}

	private def sendMetricsToGraphite(epoch: Long) {

		def sanitizeString(s: String) = s.replace(' ', '_').replace('.', '-').replace('\\', '-')

		def sanitizeStringList(list: List[String]) = list.map(sanitizeString)

		def sendToGraphite(metricPath: MetricPath, value: Long) {
			val message = "%s %s %s\n".format(metricPath.toString, value.toString, epoch.toString)
			val buffer = message.getBytes
			val packet = new DatagramPacket(buffer, buffer.length, address)
			socket.send(packet)
		}

		def sendUserMetrics(scenarioName: String, userMetric: UserMetric) = {
			val rootPath = MetricPath(List("users", sanitizeString(scenarioName)))
			sendToGraphite(rootPath + "active", userMetric.active)
			sendToGraphite(rootPath + "waiting", userMetric.waiting)
			sendToGraphite(rootPath + "done", userMetric.done)
		}

		def sendMetrics(metricPath: MetricPath, metrics: Metrics) = {
			sendToGraphite(metricPath + "count", metrics.count)

			if (metrics.count > 0L) {
				sendToGraphite(metricPath + "max", metrics.max)
				sendToGraphite(metricPath + "min", metrics.min)
				sendToGraphite(metricPath + percentiles1Name, metrics.getQuantile(percentiles1))
				sendToGraphite(metricPath + percentiles2Name, metrics.getQuantile(percentiles2))
			}
		}

		def sendRequestMetrics(path: List[String], requestMetrics: RequestMetrics) = {
			val rootPath = MetricPath(sanitizeStringList(path))

			val (okMetrics, koMetrics, allMetrics) = requestMetrics.metrics

			sendMetrics(rootPath + "ok", okMetrics)
			sendMetrics(rootPath + "ko", koMetrics)
			sendMetrics(rootPath + "all", allMetrics)

			requestMetrics.reset
		}

		try {
			if (socket == null) socket = newSocket

			sendUserMetrics("allUsers", allUsers)
			usersPerScenario.foreach {
				case (scenarioName, userMetric) => sendUserMetrics(scenarioName, userMetric)
			}
			sendRequestMetrics(List("allRequests"), allRequests)
			perRequest.foreach {
				case (path, requestMetric) => sendRequestMetrics(path, requestMetric)
			}

		} catch {
			case e: IOException => {
				error("Error writing to Graphite", e)
				if (socket != null) {
					try {
						socket.close
					} catch {
						case _: Exception => // shut up
					} finally {
						socket = null
					}
				}
			}
		}
	}

	private class SendToGraphiteTask extends TimerTask {
		def run {
			self ! SendToGraphite
		}
	}

	private object MetricPath {

		def apply(elements: List[String]) = new MetricPath(metricRootPath ::: elements)
	}

	private class MetricPath(path: List[String]) {

		def +(element: String) = new MetricPath(path ::: List(element))

		def +(elements: List[String]) = new MetricPath(path ::: elements)

		override def toString = path.mkString(".")
	}
}

