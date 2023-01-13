/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.datadog

import java.time.Instant

import scala.collection.convert.ImplicitConversions.`map AsScalaConcurrentMap`

import io.gatling.commons.util.Clock
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.writer.DataWriterMessage.LoadEvent
import io.gatling.core.stats.writer.DataWriterMessage.LoadEvent.{ Error => EventError, Response, UserEnd, UserStart }
import io.gatling.core.stats.writer.{ DataWriter, DataWriterMessage }
import io.gatling.core.util.NameGen
import io.gatling.datadog.DatadogRequests.{ sendRequestLatencySeconds, sendTotalErrors, sendTotalFinishedUsers, sendTotalStartedUsers }
import scaladog.api.metrics.{ MetricType, Point, Series }

private[gatling] class DatadogDataWriter(
    clock: Clock,
    configuration: GatlingConfiguration
) extends DataWriter[DatadogData]
    with NameGen {

  private val client = scaladog.Client(
    configuration.
  )

  override def onInit(init: DataWriterMessage.Init): DatadogData =
    DatadogData.initialise(init)

  override def onFlush(data: DatadogData): Unit = {
    sendTotalStartedUsers(data.simulation, data.startedUsers.toList)
    sendTotalFinishedUsers(data.simulation, data.finishedUsers.toList)
    sendTotalErrors(data.simulation, data.errorCounter.toList)
    sendRequestLatencySeconds(data.simulation, data.requestLatency.toList)
  }

  override def onCrash(cause: String, data: DatadogData): Unit =
    logger.error(cause)

  override def onStop(data: DatadogData): Unit =
    onFlush(data)

  override def onMessage(message: LoadEvent, data: DatadogData): Unit =
    message match {
      case userStart: UserStart => onUserStartMessage(userStart, data)
      case userEnd: UserEnd     => onUserEndMessage(userEnd, data)
      case response: Response   => onResponseMessage(response, data)
      case error: EventError    => onErrorMessage(error, data)
      case _                    => ()
    }

  private def onUserStartMessage(user: UserStart, data: DatadogData): Unit =
    data.startedUsers.put(UserLabels(currentInstant, user.scenario), 1)

  private def onUserEndMessage(user: UserEnd, data: DatadogData): Unit =
    data.finishedUsers.put(UserLabels(currentInstant, user.scenario), 1)

  private def onResponseMessage(response: Response, data: DatadogData): Unit =
    data.requestLatency.put(
      ResponseLabels(
        currentInstant,
        response.scenario,
        response.status.toString
      ),
      (response.endTimestamp - response.startTimestamp) / 1000d
    )

  private def onErrorMessage(error: EventError, data: DatadogData): Unit =
    data.errorCounter.put(ErrorLabels(currentInstant), 1)

  private def currentInstant = Instant.ofEpochMilli(clock.nowMillis)
}

object DatadogRequests {
  val host = "somehost"

  def sendTotalStartedUsers(
      simulation: String,
      startedUsers: List[(UserLabels, Int)]
  ): Unit = {
    val groupedByScenario: Map[String, List[(UserLabels, Int)]] =
      startedUsers.groupBy(_._1.scenario)
    groupedByScenario.foreachEntry { (scenario: String, users: List[(UserLabels, Int)]) =>
      val points = users.map { case (labels: UserLabels, value: Int) =>
        Point(labels.instant, BigDecimal.valueOf(value))
      }
      val response = scaladog
        .Client()
        .metrics
        .postMetrics(
          Seq(
            Series(
              metric = "total_started_users",
              points = points,
              host = host,
              tags = Seq(s"simulation:$simulation,scenario:$scenario"),
              metricType = MetricType.Count
            )
          )
        )
      response.status
      ()
    }
  }

  def sendTotalFinishedUsers(
      simulation: String,
      finishedUsers: List[(UserLabels, Int)]
  ): Unit = {
    val groupedByScenario: Map[String, List[(UserLabels, Int)]] =
      finishedUsers.groupBy(_._1.scenario)
    groupedByScenario.foreachEntry { (scenario: String, users: List[(UserLabels, Int)]) =>
      val points = users.map { case (labels: UserLabels, value: Int) =>
        Point(labels.instant, BigDecimal.valueOf(value))
      }
      val response = scaladog
        .Client()
        .metrics
        .postMetrics(
          Seq(
            Series(
              metric = "total_finished_users",
              points = points,
              host = host,
              tags = Seq(s"simulation:$simulation,scenario:$scenario"),
              metricType = MetricType.Count
            )
          )
        )
      response.status
      ()
    }
  }

  def sendTotalErrors(
      simulation: String,
      errors: List[(ErrorLabels, Int)]
  ): Unit = {
    val points = errors.map { case (labels: ErrorLabels, value: Int) =>
      Point(labels.instant, BigDecimal.valueOf(value))
    }
    val response = scaladog
      .Client()
      .metrics
      .postMetrics(
        Seq(
          Series(
            metric = "total_errors",
            points = points,
            host = host,
            tags = Seq(s"simulation:$simulation"),
            metricType = MetricType.Count
          )
        )
      )
    response.status
    ()
  }

  def sendRequestLatencySeconds(
      simulation: String,
      requestLatency: List[(ResponseLabels, Double)]
  ): Unit = {
    val groupedByScenario: Map[String, List[(ResponseLabels, Double)]] =
      requestLatency.groupBy(_._1.scenario)
    groupedByScenario.foreachEntry { (scenario: String, requests: List[(ResponseLabels, Double)]) =>
      val groupedByStatus =
        requests.groupBy(_._1.status)
      groupedByStatus.foreachEntry { (status: String, users: List[(ResponseLabels, Double)]) =>
        val points = users.map { case (labels: ResponseLabels, value: Double) =>
          Point(labels.instant, BigDecimal.valueOf(value))
        }
        val response = scaladog
          .Client()
          .metrics
          .postMetrics(
            Seq(
              Series(
                metric = "request_latency_seconds",
                points = points,
                host = host,
                tags = Seq(
                  s"simulation:$simulation,scenario:$scenario,status$status"
                ),
                metricType = MetricType.Gauge
              )
            )
          )
        response.status
        ()
      }
    }
  }
}
