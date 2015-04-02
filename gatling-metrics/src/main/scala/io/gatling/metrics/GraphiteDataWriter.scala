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

import io.gatling.core.config.GatlingConfiguration
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
}

case class GraphiteData(configuration: GatlingConfiguration,
                        metricRootPath: String,
                        metricsSender: ActorRef,
                        requestsByPath: mutable.Map[GraphitePath, RequestMetricsBuffer],
                        usersByScenario: mutable.Map[GraphitePath, UsersBreakdownBuffer],
                        percentiles1Name: String,
                        percentiles2Name: String,
                        percentiles3Name: String,
                        percentiles4Name: String) extends DataWriterData

private[gatling] class GraphiteDataWriter extends DataWriter[GraphiteData] {
  import GraphiteDataWriter._
  import GraphitePath._

  def newRequestMetricsBuffer(configuration: GatlingConfiguration): RequestMetricsBuffer =
    new TDigestRequestMetricsBuffer(configuration)

  private val flushTimerName = "flushTimer"

  def onInit(init: Init, controller: ActorRef): GraphiteData = {
    import init._
    val metricRootPath = configuration.data.graphite.rootPathPrefix + "." + sanitizeString(runMessage.simulationId) + "."
    val metricsSender: ActorRef = context.actorOf(MetricsSender.props(configuration), actorName("metricsSender"))
    val requestsByPath = mutable.Map.empty[GraphitePath, RequestMetricsBuffer]
    val usersByScenario = mutable.Map.empty[GraphitePath, UsersBreakdownBuffer]
    val percentiles1Name = "percentiles" + configuration.charting.indicators.percentile1
    val percentiles2Name = "percentiles" + configuration.charting.indicators.percentile2
    val percentiles3Name = "percentiles" + configuration.charting.indicators.percentile3
    val percentiles4Name = "percentiles" + configuration.charting.indicators.percentile4

    usersByScenario.update(AllUsersKey, new UsersBreakdownBuffer(scenarios.map(_.nbUsers).sum))
    scenarios.foreach(scenario => usersByScenario += (UsersRootKey / scenario.name) -> new UsersBreakdownBuffer(scenario.nbUsers))

    setTimer(flushTimerName, Flush, configuration.data.graphite.writeInterval seconds, repeat = true)

    GraphiteData(configuration, metricRootPath, metricsSender, requestsByPath, usersByScenario, percentiles1Name, percentiles2Name, percentiles3Name, percentiles4Name)
  }

  def onFlush(data: GraphiteData): Unit = {
    import data._

    val requestsMetrics = requestsByPath.mapValues(_.metricsByStatus).toMap
    val usersBreakdowns = usersByScenario.mapValues(UsersBreakdown(_)).toMap

    // Reset all metrics
    requestsByPath.foreach { case (_, buff) => buff.clear() }

    sendMetricsToGraphite(data, nowSeconds, requestsMetrics, usersBreakdowns)
  }

  private def onUserMessage(userMessage: UserMessage, data: GraphiteData): Unit = {
    import data._
    usersByScenario(UsersRootKey / userMessage.scenario).add(userMessage)
    usersByScenario(AllUsersKey).add(userMessage)
  }

  private def onRequestMessage(request: RequestEndMessage, data: GraphiteData): Unit = {
    import data._
    if (!configuration.data.graphite.light) {
      val path = graphitePath(request.groupHierarchy :+ request.name)
      requestsByPath.getOrElseUpdate(path, newRequestMetricsBuffer(configuration)).add(request.status, request.timings.responseTime)
    }
    requestsByPath.getOrElseUpdate(AllRequestsKey, newRequestMetricsBuffer(configuration)).add(request.status, request.timings.responseTime)
  }

  override def onMessage(message: LoadEventMessage, data: GraphiteData): Unit = message match {
    case user: UserMessage          => onUserMessage(user, data)
    case request: RequestEndMessage => onRequestMessage(request, data)
    case _                          =>
  }

  def onTerminate(data: GraphiteData): Unit = cancelTimer(flushTimerName)

  private def sendMetricsToGraphite(data: GraphiteData,
                                    epoch: Long,
                                    requestsMetrics: Map[GraphitePath, MetricByStatus],
                                    usersBreakdowns: Map[GraphitePath, UsersBreakdown]): Unit = {

    for ((metricPath, usersBreakdown) <- usersBreakdowns) sendUserMetrics(data, metricPath, usersBreakdown, epoch)

    if (data.configuration.data.graphite.light)
      requestsMetrics.get(AllRequestsKey).foreach(allRequestsMetric => sendRequestMetrics(data, AllRequestsKey, allRequestsMetric, epoch))
    else
      for ((path, requestMetric) <- requestsMetrics) sendRequestMetrics(data, path, requestMetric, epoch)

  }

  private def sendRequestMetrics(data: GraphiteData, metricPath: GraphitePath, metricByStatus: MetricByStatus, epoch: Long): Unit = {
    sendMetrics(data, metricPath / "ok", metricByStatus.ok, epoch)
    sendMetrics(data, metricPath / "ko", metricByStatus.ko, epoch)
    sendMetrics(data, metricPath / "all", metricByStatus.all, epoch)
  }

  private def sendUserMetrics(data: GraphiteData, userMetricPath: GraphitePath, userMetric: UsersBreakdown, epoch: Long): Unit = {
    sendToGraphite(data, userMetricPath / "active", userMetric.active, epoch)
    sendToGraphite(data, userMetricPath / "waiting", userMetric.waiting, epoch)
    sendToGraphite(data, userMetricPath / "done", userMetric.done, epoch)
  }

  private def sendMetrics(data: GraphiteData, metricPath: GraphitePath, metrics: Option[Metrics], epoch: Long): Unit =
    metrics match {
      case None => sendToGraphite(data, metricPath / "count", 0, epoch)
      case Some(m) =>
        import data._
        sendToGraphite(data, metricPath / "count", m.count, epoch)
        sendToGraphite(data, metricPath / "max", m.max, epoch)
        sendToGraphite(data, metricPath / "min", m.min, epoch)
        sendToGraphite(data, metricPath / percentiles1Name, m.percentile1, epoch)
        sendToGraphite(data, metricPath / percentiles2Name, m.percentile2, epoch)
        sendToGraphite(data, metricPath / percentiles3Name, m.percentile3, epoch)
        sendToGraphite(data, metricPath / percentiles4Name, m.percentile4, epoch)
    }

  private def sendToGraphite[T: Numeric](data: GraphiteData, metricPath: GraphitePath, value: T, epoch: Long): Unit = {
    import data._
    metricsSender ! SendMetric(metricPath.pathKeyWithPrefix(metricRootPath), value, epoch)
  }
}
