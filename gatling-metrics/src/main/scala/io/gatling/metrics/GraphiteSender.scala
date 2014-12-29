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

import akka.actor.{ ActorRef, Terminated }
import akka.actor.ActorDSL.actor

import io.gatling.core.akka.BaseActor
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.TimeHelper.nowSeconds
import io.gatling.metrics.message._
import io.gatling.metrics.sender.MetricsSender
import io.gatling.metrics.types._

private[metrics] class GraphiteSender(rootPath: String)(implicit configuration: GatlingConfiguration) extends BaseActor {
  import io.gatling.metrics.GraphiteDataWriter._

  private val percentiles1Name = "percentiles" + configuration.charting.indicators.percentile1
  private val percentiles2Name = "percentiles" + configuration.charting.indicators.percentile2
  private val percentiles3Name = "percentiles" + configuration.charting.indicators.percentile3
  private val percentiles4Name = "percentiles" + configuration.charting.indicators.percentile4

  private var metricsSender: ActorRef = _

  def receive: Receive = {
    case SendMetrics(requestsMetrics, usersBreakdowns) =>
      sendMetricsToGraphite(nowSeconds, requestsMetrics, usersBreakdowns)
    case Terminated(_) =>
      throw new IllegalStateException("Metrics sender failed, restarting GraphiteSender.")
  }

  private def sendMetricsToGraphite(epoch: Long,
                                    requestsMetrics: Map[GraphitePath, MetricByStatus],
                                    usersBreakdowns: Map[GraphitePath, UsersBreakdown]): Unit = {

    if (metricsSender == null) {
      metricsSender = actor(context, actorName("metricsSender"))(MetricsSender.newMetricsSender)
      context watch metricsSender
    }

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
    metricsSender ! SendMetric(metricPath.pathKeyWithPrefix(rootPath), value, epoch)
}
