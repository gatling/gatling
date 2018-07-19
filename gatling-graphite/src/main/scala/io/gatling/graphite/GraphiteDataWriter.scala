/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.graphite

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Collections._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.stats.writer._
import io.gatling.core.util.NameGen
import io.gatling.graphite.message.GraphiteMetrics
import io.gatling.graphite.sender.MetricsSender
import io.gatling.graphite.types._

import akka.actor.ActorRef

case class GraphiteData(
    metricsSender:   ActorRef,
    requestsByPath:  mutable.Map[GraphitePath, RequestMetricsBuffer],
    usersByScenario: mutable.Map[GraphitePath, UserBreakdownBuffer],
    format:          GraphitePathPattern
) extends DataWriterData

private[gatling] class GraphiteDataWriter(clock: Clock, configuration: GatlingConfiguration) extends DataWriter[GraphiteData] with NameGen {

  def newResponseMetricsBuffer: RequestMetricsBuffer =
    new HistogramRequestMetricsBuffer(configuration)

  private val flushTimerName = "flushTimer"

  def onInit(init: Init): GraphiteData = {
    import init._

    val metricsSender: ActorRef = context.actorOf(MetricsSender.props(clock, configuration), genName("metricsSender"))
    val requestsByPath = mutable.Map.empty[GraphitePath, RequestMetricsBuffer]
    val usersByScenario = mutable.Map.empty[GraphitePath, UserBreakdownBuffer]

    val pattern: GraphitePathPattern = new OldGraphitePathPattern(runMessage, configuration)

    usersByScenario.update(pattern.allUsersPath, new UserBreakdownBuffer(scenarios.sumBy(_.totalUserCount.getOrElse(0L))))
    scenarios.foreach(scenario => usersByScenario += (pattern.usersPath(scenario.name) -> new UserBreakdownBuffer(scenario.totalUserCount.getOrElse(0L))))

    setTimer(flushTimerName, Flush, configuration.data.graphite.writeInterval seconds, repeat = true)

    GraphiteData(metricsSender, requestsByPath, usersByScenario, pattern)
  }

  def onFlush(data: GraphiteData): Unit = {
    import data._

    val requestsMetrics = requestsByPath.mapValues(_.metricsByStatus).toMap
    val usersBreakdowns = usersByScenario.mapValues(_.breakDown).toMap

    // Reset all metrics
    requestsByPath.foreach { case (_, buff) => buff.clear() }

    sendMetricsToGraphite(data, clock.nowSeconds, requestsMetrics, usersBreakdowns)
  }

  private def onUserMessage(userMessage: UserMessage, data: GraphiteData): Unit = {
    import data._
    usersByScenario(format.usersPath(userMessage.session.scenario)).add(userMessage)
    usersByScenario(format.allUsersPath).add(userMessage)
  }

  private def onResponseMessage(response: ResponseMessage, data: GraphiteData): Unit = {
    import data._
    import response._
    val responseTime = ResponseTimings.responseTime(startTimestamp, endTimestamp)
    if (!configuration.data.graphite.light) {
      requestsByPath.getOrElseUpdate(format.responsePath(name, groupHierarchy), newResponseMetricsBuffer).add(status, responseTime)
    }
    requestsByPath.getOrElseUpdate(format.allResponsesPath, newResponseMetricsBuffer).add(status, responseTime)
  }

  override def onMessage(message: LoadEventMessage, data: GraphiteData): Unit = message match {
    case user: UserMessage         => onUserMessage(user, data)
    case response: ResponseMessage => onResponseMessage(response, data)
    case _                         =>
  }

  override def onCrash(cause: String, data: GraphiteData): Unit = {}

  def onStop(data: GraphiteData): Unit = cancelTimer(flushTimerName)

  private def sendMetricsToGraphite(
    data:            GraphiteData,
    epoch:           Long,
    requestsMetrics: Map[GraphitePath, MetricByStatus],
    userBreakdowns:  Map[GraphitePath, UserBreakdown]
  ): Unit = {

    import data._
    metricsSender ! GraphiteMetrics(format.metrics(userBreakdowns, requestsMetrics), epoch)
  }
}
