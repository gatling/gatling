/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.metrics

import scala.collection.mutable

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.writer._
import io.gatling.metrics.message.StatsdMetrics
import io.gatling.metrics.sender.MetricsSender
import io.gatling.metrics.types._

import akka.actor.ActorRef

case class StatsdData(
  configuration: GatlingConfiguration,
  metricsSender: ActorRef,
  format:        GraphitePathPattern
) extends DataWriterData

private[gatling] class StatsdDataWriter extends DataWriter[StatsdData] {

  def newResponseMetricsBuffer(configuration: GatlingConfiguration): RequestMetricsBuffer =
    new HistogramRequestMetricsBuffer(configuration)

  def onInit(init: Init): StatsdData = {
    import init._

    val metricsSender: ActorRef = context.actorOf(MetricsSender.statsdProps(configuration), actorName("metricsSender"))
    val requestsByPath = mutable.Map.empty[GraphitePath, RequestMetricsBuffer]
    val usersByScenario = mutable.Map.empty[GraphitePath, UserBreakdownBuffer]

    val pattern: GraphitePathPattern = new OldGraphitePathPattern(runMessage, configuration)

    usersByScenario.update(pattern.allUsersPath, new UserBreakdownBuffer(scenarios.map(_.userCount).sum))
    scenarios.foreach(scenario => usersByScenario += (pattern.usersPath(scenario.name) -> new UserBreakdownBuffer(scenario.userCount)))

    StatsdData(configuration, metricsSender, pattern)
  }

  def onFlush(data: StatsdData): Unit = {}

  private def onResponseMessage(response: ResponseMessage, data: StatsdData): Unit = {
    import data._
    import response._

    if (!configuration.data.statsd.light) {
      sendMetricsToStatsd(data, format.responsePath(name, groupHierarchy).pathKey, timings.responseTime, "ms")
    }
    sendMetricsToStatsd(data, format.allResponsesPath.pathKey, timings.responseTime, "ms")
  }

  override def onMessage(message: LoadEventMessage, data: StatsdData): Unit = message match {
    case response: ResponseMessage => onResponseMessage(response, data)
    case _                         =>
  }

  override def onCrash(cause: String, data: StatsdData): Unit = {}

  override def onStop(data: StatsdData): Unit = {}

  private def sendMetricsToStatsd(
    data:     StatsdData,
    name:     String,
    value:    Long,
    statType: String
  ): Unit = {

    import data._

    metricsSender ! StatsdMetrics(name, value, statType)
  }

}
