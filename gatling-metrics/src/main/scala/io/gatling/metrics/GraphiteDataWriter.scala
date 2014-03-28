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
package io.gatling.metrics

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

import akka.actor.{ ActorRef, actorRef2Scala }
import akka.actor.ActorDSL.actor
import io.gatling.core.akka.BaseActor
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingConfiguration.{ configuration => gatlingConfiguration }
import io.gatling.core.result.writer.{ DataWriter, GroupMessage, RequestMessage, RunMessage, ShortScenarioDescription, UserMessage }
import io.gatling.core.util.TimeHelper.nowSeconds
import io.gatling.metrics.sender.MetricsSender
import io.gatling.metrics.types.{ MetricByStatus, Metrics, RequestMetricsBuffer, UsersBreakdown, UsersBreakdownBuffer }

object GraphiteDataWriter {
  val allRequestsKey = List("allRequests")
  val allUsersKey = "allUsers"
}

class GraphiteDataWriter extends DataWriter {

  import GraphiteDataWriter.{ allRequestsKey, allUsersKey }

  implicit val configuration = gatlingConfiguration

  private var graphiteSender: ActorRef = _

  private val requestsByPath = mutable.Map.empty[List[String], RequestMetricsBuffer]
  private val usersByScenario = mutable.Map.empty[String, UsersBreakdownBuffer]

  def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription]): Unit = {

    val metricRootPath = configuration.data.graphite.rootPathPrefix + "." + run.simulationId + "."

    graphiteSender = actor(context)(new GraphiteSender(metricRootPath))

    usersByScenario.update(allUsersKey, new UsersBreakdownBuffer(scenarios.map(_.nbUsers).sum))
    scenarios.foreach(scenario => usersByScenario += scenario.name -> new UsersBreakdownBuffer(scenario.nbUsers))
    scheduler.schedule(0 millisecond, 1 second, self, Send)
  }

  def onUserMessage(userMessage: UserMessage): Unit = {
    usersByScenario(userMessage.scenarioName).add(userMessage)
    usersByScenario(allUsersKey).add(userMessage)
  }

  def onGroupMessage(group: GroupMessage): Unit = {}

  def onRequestMessage(request: RequestMessage): Unit = {
    if (!configuration.data.graphite.light) {
      val path = (request.name :: request.groupStack.map(_.name)).reverse
      requestsByPath.getOrElseUpdate(path, new RequestMetricsBuffer).add(request.status, request.responseTime)
    }
    requestsByPath.getOrElseUpdate(allRequestsKey, new RequestMetricsBuffer).add(request.status, request.responseTime)
  }

  def onTerminateDataWriter(): Unit = {
    graphiteSender ! Flush
  }

  override def receive = uninitialized

  override def initialized: Receive = super.initialized.orElse {
    case Send => {
      val requestMetrics = requestsByPath.mapValues(_.metricsByStatus).toMap
      val currentUserBreakdowns = usersByScenario.mapValues(UsersBreakdown(_)).toMap

      // Reset all metrics related to 
      requestsByPath.clear()

      graphiteSender.forward(SendMetrics(requestMetrics, currentUserBreakdowns))
    }
  }
}

case object Flush
case object Send
case class SendMetrics(requestMetrics: Map[List[String], MetricByStatus], usersBreakdowns: Map[String, UsersBreakdown])

private class GraphiteSender(metricRootPath: String)(implicit configuration: GatlingConfiguration) extends BaseActor {

  import GraphiteDataWriter.{ allRequestsKey, allUsersKey }

  private val percentiles1Name = "percentiles" + configuration.charting.indicators.percentile1
  private val percentiles2Name = "percentiles" + configuration.charting.indicators.percentile2

  private val sanitizeStringMemo = mutable.Map.empty[String, String]
  private val sanitizeStringListMemo = mutable.Map.empty[List[String], List[String]]
  private var metricsSender: MetricsSender = _

  override def preStart() {
    metricsSender = MetricsSender.newMetricsSender
  }

  def receive = {
    case SendMetrics(requestsMetrics, usersBreakdowns) => sendMetricsToGraphite(nowSeconds, requestsMetrics, usersBreakdowns)
    case Flush                                         => metricsSender.flush()
  }

  private def sendMetricsToGraphite(epoch: Long, requestsMetrics: Map[List[String], MetricByStatus], usersBreakdowns: Map[String, UsersBreakdown]): Unit = {

      def sanitizeString(s: String) = sanitizeStringMemo.getOrElseUpdate(s, s.replace(' ', '_').replace('.', '-').replace('\\', '-'))

      def sanitizeStringList(list: List[String]) = sanitizeStringListMemo.getOrElseUpdate(list, list.map(sanitizeString))

      def sendToGraphite(metricPath: MetricPath, value: Long) = metricsSender.sendToGraphite(metricPath.toString, value, epoch)
      def sendFloatToGraphite(metricPath: MetricPath, value: Double) = metricsSender.sendToGraphite(metricPath.toString, value, epoch)

      def sendUserMetrics(scenarioName: String, userMetric: UsersBreakdown): Unit = {
        val rootPath = MetricPath(List("users", sanitizeString(scenarioName)))
        sendToGraphite(rootPath + "active", userMetric.active)
        sendToGraphite(rootPath + "waiting", userMetric.waiting)
        sendToGraphite(rootPath + "done", userMetric.done)
      }

      def sendMetrics(metricPath: MetricPath, metrics: Option[Metrics]): Unit =
        metrics match {
          case None => sendToGraphite(metricPath + "count", 0)
          case Some(m) =>
            sendToGraphite(metricPath + "count", m.count)
            sendFloatToGraphite(metricPath + "max", m.max)
            sendFloatToGraphite(metricPath + "min", m.min)
            sendFloatToGraphite(metricPath + percentiles1Name, m.percentile1)
            sendFloatToGraphite(metricPath + percentiles2Name, m.percentile2)
        }

      def sendRequestMetrics(path: List[String], metricByStatus: MetricByStatus): Unit = {
        val metricPath = MetricPath(sanitizeStringList(path))
        sendMetrics(metricPath + "ok", metricByStatus.ok)
        sendMetrics(metricPath + "ko", metricByStatus.ko)
        sendMetrics(metricPath + "all", metricByStatus.all)
      }

    for ((scenarioName, usersBreakdown) <- usersBreakdowns) sendUserMetrics(scenarioName, usersBreakdown)

    if (configuration.data.graphite.light)
      sendRequestMetrics(allRequestsKey, requestsMetrics(allRequestsKey))
    else
      for ((path, requestMetric) <- requestsMetrics) sendRequestMetrics(path, requestMetric)

    metricsSender.flush()
  }

  private case class MetricPath(path: List[String]) {

    def +(element: String) = new MetricPath(path :+ element)

    override def toString = path.mkString(metricRootPath, ".", "")
  }
}

