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
package io.gatling.metrics.statsd

import akka.actor.ActorRef
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.message.{ End, Start }
import io.gatling.core.stats.writer._
import io.gatling.metrics.message.StatsdMetrics
import io.gatling.metrics.sender.MetricsSender
import io.gatling.metrics.types._

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

    scenarios.foreach(scenario =>
      StatsdMetrics.gauge(metricsSender, data.format.usersPath(scenario.name).bucket, scenario.userCount.toString))

    data
  }

  def onFlush(data: StatsdData): Unit = {}

  private def onUserMessage(userMessage: UserMessage, data: StatsdData): Unit = {
    import data._
    import format._

    val userEventMetricRoot = usersPath(userMessage.session.scenario)

    userMessage.event match {
      case Start =>
        StatsdMetrics.incrementGauge(metricsSender, activeUsers(userEventMetricRoot).bucket)
        StatsdMetrics.decrementGauge(metricsSender, waitingUsers(userEventMetricRoot).bucket)
      case End =>
        StatsdMetrics.incrementGauge(metricsSender, doneUsers(userEventMetricRoot).bucket)
        StatsdMetrics.decrementGauge(metricsSender, activeUsers(userEventMetricRoot).bucket)
    }

    StatsdMetrics.incrementGauge(metricsSender, usersPath(userMessage.session.scenario).bucket)
  }

  private def onResponseMessage(response: ResponseMessage, data: StatsdData): Unit = {
    import data._
    import format._
    import response._

    val responseSampleRate = configuration.data.statsd.responseSampleRate

    StatsdMetrics.timing(metricsSender, (responsePath(name, groupHierarchy) add status.name).bucket, timings.responseTime, responseSampleRate)
  }

  override def onMessage(message: LoadEventMessage, data: StatsdData): Unit = message match {
    case user: UserMessage         => onUserMessage(user, data)
    case response: ResponseMessage => onResponseMessage(response, data)
    case _                         =>
  }

  override def onCrash(cause: String, data: StatsdData): Unit = {}

  override def onStop(data: StatsdData): Unit = {}

}
