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

import io.gatling.core.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.writer._
import io.gatling.core.util.TimeHelper.nowSeconds
import io.gatling.metrics.message._
import io.gatling.metrics.sender.MetricsSender
import io.gatling.metrics.types._

private[metrics] object GraphiteDataWriter {
  import GraphitePath._
  val AllRequestsKey = graphitePath("allRequests")
  val UsersRootKey = graphitePath("users")
  val AllUsersKey = UsersRootKey / "allUsers"

  private val percentiles1Name = "percentiles" + configuration.charting.indicators.percentile1
  private val percentiles2Name = "percentiles" + configuration.charting.indicators.percentile2
  private val percentiles3Name = "percentiles" + configuration.charting.indicators.percentile3
  private val percentiles4Name = "percentiles" + configuration.charting.indicators.percentile4
}

private[gatling] class GraphiteDataWriter extends DataWriter with Flushable {
  import GraphiteDataWriter._
  import GraphitePath._

  implicit val config = configuration

  private var metricRootPath: String = _

  private val metricsSender: ActorRef = actor(context, actorName("metricsSender"))(MetricsSender.newMetricsSender)
  private val requestsByPath = mutable.Map.empty[GraphitePath, RequestMetricsBuffer]
  private val usersByScenario = mutable.Map.empty[GraphitePath, UsersBreakdownBuffer]

  def onInitializeDataWriter(assertions: Seq[Assertion], run: RunMessage, scenarios: Seq[ShortScenarioDescription]): Unit = {
    metricRootPath = config.data.graphite.rootPathPrefix + "." + sanitizeString(run.simulationId) + "."

    usersByScenario.update(AllUsersKey, new UsersBreakdownBuffer(scenarios.map(_.nbUsers).sum))
    scenarios.foreach(scenario => usersByScenario += (UsersRootKey / scenario.name) -> new UsersBreakdownBuffer(scenario.nbUsers))

    scheduler.schedule(0 millisecond, configuration.data.graphite.writeInterval second, self, Flush)
  }

  override def onFlush(): Unit = {
    val requestsMetrics = requestsByPath.mapValues(_.metricsByStatus).toMap
    val usersBreakdowns = usersByScenario.mapValues(UsersBreakdown(_)).toMap

    // Reset all metrics
    requestsByPath.foreach { case (_, buff) => buff.clear() }

    sendMetricsToGraphite(nowSeconds, requestsMetrics, usersBreakdowns)
  }

  private def onUserMessage(userMessage: UserMessage): Unit = {
    usersByScenario(UsersRootKey / userMessage.scenario).add(userMessage)
    usersByScenario(AllUsersKey).add(userMessage)
  }

  private def onRequestMessage(request: RequestEndMessage): Unit = {
    if (!configuration.data.graphite.light) {
      val path = graphitePath(request.groupHierarchy :+ request.name)
      requestsByPath.getOrElseUpdate(path, new RequestMetricsBuffer).add(request.status, request.timings.responseTime)
    }
    requestsByPath.getOrElseUpdate(AllRequestsKey, new RequestMetricsBuffer).add(request.status, request.timings.responseTime)
  }

  override def onMessage(message: LoadEventMessage): Unit = message match {
    case user: UserMessage          => onUserMessage(user)
    case request: RequestEndMessage => onRequestMessage(request)
    case _                          =>
  }

  def onTerminateDataWriter(): Unit = () // Do nothing, let the ActorSystem free resources

  override def receive: Receive = uninitialized

  private def sendMetricsToGraphite(epoch: Long,
                                    requestsMetrics: Map[GraphitePath, MetricByStatus],
                                    usersBreakdowns: Map[GraphitePath, UsersBreakdown]): Unit = {

    for ((metricPath, usersBreakdown) <- usersBreakdowns) sendUserMetrics(metricPath, usersBreakdown, epoch)

    if (configuration.data.graphite.light)
      requestsMetrics.get(AllRequestsKey).foreach(allRequestsMetric => sendRequestMetrics(AllRequestsKey, allRequestsMetric, epoch))
    else
      for ((path, requestMetric) <- requestsMetrics) sendRequestMetrics(path, requestMetric, epoch)

  }

  private def sendRequestMetrics(metricPath: GraphitePath, metricByStatus: MetricByStatus, epoch: Long): Unit = {
    sendMetrics(metricPath / "ok", metricByStatus.ok, epoch)
    sendMetrics(metricPath / "ko", metricByStatus.ko, epoch)
    sendMetrics(metricPath / "all", metricByStatus.all, epoch)
  }

  private def sendUserMetrics(userMetricPath: GraphitePath, userMetric: UsersBreakdown, epoch: Long): Unit = {
    sendToGraphite(userMetricPath / "active", userMetric.active, epoch)
    sendToGraphite(userMetricPath / "waiting", userMetric.waiting, epoch)
    sendToGraphite(userMetricPath / "done", userMetric.done, epoch)
  }

  private def sendMetrics(metricPath: GraphitePath, metrics: Option[Metrics], epoch: Long): Unit =
    metrics match {
      case None => sendToGraphite(metricPath / "count", 0, epoch)
      case Some(m) =>
        sendToGraphite(metricPath / "count", m.count, epoch)
        sendToGraphite(metricPath / "max", m.max, epoch)
        sendToGraphite(metricPath / "min", m.min, epoch)
        sendToGraphite(metricPath / percentiles1Name, m.percentile1, epoch)
        sendToGraphite(metricPath / percentiles2Name, m.percentile2, epoch)
        sendToGraphite(metricPath / percentiles3Name, m.percentile3, epoch)
        sendToGraphite(metricPath / percentiles4Name, m.percentile4, epoch)
    }

  private def sendToGraphite[T: Numeric](metricPath: GraphitePath, value: T, epoch: Long): Unit =
    metricsSender ! SendMetric(metricPath.pathKeyWithPrefix(metricRootPath), value, epoch)
}
