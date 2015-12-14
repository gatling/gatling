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

import io.gatling.core.stats.message.{ End, Start }

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
  format:        StatsdMetricSeriesPattern
) extends DataWriterData

private[gatling] class StatsdDataWriter extends DataWriter[StatsdData] {

  def newResponseMetricsBuffer(configuration: GatlingConfiguration): RequestMetricsBuffer =
    new HistogramRequestMetricsBuffer(configuration)

  def onInit(init: Init): StatsdData = {
    import init._

    val metricsSender: ActorRef = context.actorOf(MetricsSender.statsdProps(configuration), actorName("metricsSender"))

    val pattern: StatsdMetricSeriesPattern = new StatsdMetricSeriesPattern(runMessage, configuration)

    val data = StatsdData(configuration, metricsSender, pattern)

    data
  }

  def onFlush(data: StatsdData): Unit = {}

  private def onSimulationStart(init: Init, data: StatsdData): Unit = {
    import data._
    import init._

    scenarios.foreach(scenario => sendMetricsToStatsd(data, format.usersPath(scenario.name).bucket, scenario.userCount, "c"))

  }

  private def onUserMessage(userMessage: UserMessage, data: StatsdData): Unit = {
    import data._
    import format._

    val userEventMetricRoot = usersPath(userMessage.session.scenario)
    val metricCountType = "c"

    userMessage.event match {
      case Start =>
        sendMetricsToStatsd(data, activeUsers(userEventMetricRoot).bucket, 1l, metricCountType)
        sendMetricsToStatsd(data, waitingUsers(userEventMetricRoot).bucket, -1l, metricCountType)
      case End =>
        sendMetricsToStatsd(data, doneUsers(userEventMetricRoot).bucket, 1l, metricCountType)
        sendMetricsToStatsd(data, activeUsers(userEventMetricRoot).bucket, -1l, metricCountType)
    }

    sendMetricsToStatsd(data, format.usersPath(userMessage.session.scenario).bucket, 1l, "c")
  }

  private def onResponseMessage(response: ResponseMessage, data: StatsdData): Unit = {
    import data._
    import response._

    sendMetricsToStatsd(data, format.responsePath(name, groupHierarchy).bucket, timings.responseTime, "ms")

  }

  override def onMessage(message: LoadEventMessage, data: StatsdData): Unit = message match {
    case user: UserMessage         => onUserMessage(user, data)
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
