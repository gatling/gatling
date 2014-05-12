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

import akka.actor.ActorRef
import akka.actor.ActorDSL.actor
import io.gatling.core.akka.BaseActor
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingConfiguration.{ configuration => gatlingConfiguration }
import io.gatling.core.result.writer.{ DataWriter, GroupMessage, RequestMessage, RunMessage, ShortScenarioDescription, UserMessage }
import io.gatling.core.util.TimeHelper.nowSeconds
import io.gatling.metrics.sender.MetricsSender
import io.gatling.metrics.types._

object GraphiteDataWriter {
  import GraphitePath._
  val allRequestsKey = graphitePath("allRequests")
  val usersRootKey = graphitePath("users")
  val allUsersKey = usersRootKey / "allUsers"
}

class GraphiteDataWriter extends DataWriter {
  import GraphiteDataWriter._
  import GraphitePath._

  implicit val configuration = gatlingConfiguration

  private var graphiteSender: ActorRef = _

  private val requestsByPath = mutable.Map.empty[GraphitePath, RequestMetricsBuffer]
  private val usersByScenario = mutable.Map.empty[GraphitePath, UsersBreakdownBuffer]

  def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription]): Unit = {
    val metricRootPath = configuration.data.graphite.rootPathPrefix + "." + sanitizeString(run.simulationId) + "."
    graphiteSender = actor(context)(new GraphiteSender(metricRootPath))

    usersByScenario.update(allUsersKey, new UsersBreakdownBuffer(scenarios.map(_.nbUsers).sum))
    scenarios.foreach(scenario => usersByScenario += (usersRootKey / scenario.name) -> new UsersBreakdownBuffer(scenario.nbUsers))

    scheduler.schedule(0 millisecond, 1 second, self, Send)
  }

  def onUserMessage(userMessage: UserMessage): Unit = {
    usersByScenario(usersRootKey / userMessage.scenarioName).add(userMessage)
    usersByScenario(allUsersKey).add(userMessage)
  }

  def onGroupMessage(group: GroupMessage): Unit = {}

  def onRequestMessage(request: RequestMessage): Unit = {
    if (!configuration.data.graphite.light) {
      val path = graphitePath(request.groupHierarchy :+ request.name)
      requestsByPath.getOrElseUpdate(path, new RequestMetricsBuffer).add(request.status, request.responseTime)
    }
    requestsByPath.getOrElseUpdate(allRequestsKey, new RequestMetricsBuffer).add(request.status, request.responseTime)
  }

  def onTerminateDataWriter(): Unit = graphiteSender ! Flush

  override def receive: Receive = uninitialized

  override def initialized: Receive = super.initialized.orElse {
    case Send =>
      val requestMetrics = requestsByPath.mapValues(_.metricsByStatus).toMap
      val currentUserBreakdowns = usersByScenario.mapValues(UsersBreakdown(_)).toMap

      // Reset all metrics
      requestsByPath.foreach { case (_, buff) => buff.clear() }

      graphiteSender ! SendMetrics(requestMetrics, currentUserBreakdowns)
  }
}

private class GraphiteSender(graphiteRootPathKey: String)(implicit configuration: GatlingConfiguration) extends BaseActor {
  import GraphiteDataWriter._

  private val percentiles1Name = "percentiles" + configuration.charting.indicators.percentile1
  private val percentiles2Name = "percentiles" + configuration.charting.indicators.percentile2

  private var metricsSender: MetricsSender = _

  override def preStart(): Unit = {
    metricsSender = MetricsSender.newMetricsSender
  }

  def receive: Receive = {
    case SendMetrics(requestsMetrics, usersBreakdowns) => sendMetricsToGraphite(nowSeconds, requestsMetrics, usersBreakdowns)
    case Flush                                         => metricsSender.flush()
  }

  private def sendMetricsToGraphite(epoch: Long,
                                    requestsMetrics: Map[GraphitePath, MetricByStatus],
                                    usersBreakdowns: Map[GraphitePath, UsersBreakdown]): Unit = {

      def sendToGraphite[T: Numeric](metricPath: GraphitePath, value: T): Unit =
        metricsSender.sendToGraphite(metricPath.pathKeyWithPrefix(graphiteRootPathKey), value, epoch)

      def sendUserMetrics(userMetricPath: GraphitePath, userMetric: UsersBreakdown): Unit = {
        sendToGraphite(userMetricPath / "active", userMetric.active)
        sendToGraphite(userMetricPath / "waiting", userMetric.waiting)
        sendToGraphite(userMetricPath / "done", userMetric.done)
      }

      def sendMetrics(metricPath: GraphitePath, metrics: Option[Metrics]): Unit =
        metrics match {
          case None => sendToGraphite(metricPath / "count", 0)
          case Some(m) =>
            sendToGraphite(metricPath / "count", m.count)
            sendToGraphite(metricPath / "max", m.max)
            sendToGraphite(metricPath / "min", m.min)
            sendToGraphite(metricPath / percentiles1Name, m.percentile1)
            sendToGraphite(metricPath / percentiles2Name, m.percentile2)
        }

      def sendRequestMetrics(metricPath: GraphitePath, metricByStatus: MetricByStatus): Unit = {
        sendMetrics(metricPath / "ok", metricByStatus.ok)
        sendMetrics(metricPath / "ko", metricByStatus.ko)
        sendMetrics(metricPath / "all", metricByStatus.all)
      }

    for ((metricPath, usersBreakdown) <- usersBreakdowns) sendUserMetrics(metricPath, usersBreakdown)

    if (configuration.data.graphite.light)
      requestsMetrics.get(allRequestsKey).foreach(allRequestsMetric => sendRequestMetrics(allRequestsKey, allRequestsMetric))
    else
      for ((path, requestMetric) <- requestsMetrics) sendRequestMetrics(path, requestMetric)

    metricsSender.flush()
  }

}

