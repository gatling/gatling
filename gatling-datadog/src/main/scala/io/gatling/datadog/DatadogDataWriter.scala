/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import scala.concurrent.{ blocking, ExecutionContext, Future }
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala
import scala.util.{ Failure, Success }

import io.gatling.commons.util.Clock
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.writer.{ DataWriter, DataWriterMessage }
import io.gatling.core.stats.writer.DataWriterMessage.LoadEvent
import io.gatling.core.stats.writer.DataWriterMessage.LoadEvent.{ Error => EventError, Response, UserEnd, UserStart }
import io.gatling.core.util.NameGen
import io.gatling.datadog.DatadogRequests.{ sendRequestLatencySeconds, sendTotalErrors, sendTotalFinishedUsers, sendTotalStartedUsers }

import scaladog.Client
import scaladog.api.{ DatadogSite, StatusResponse }
import scaladog.api.metrics.{ MetricType, Point, Series }

private[gatling] class DatadogDataWriter(
                                          clock: Clock,
                                          configuration: GatlingConfiguration
                                        ) extends DataWriter[DatadogData]
  with NameGen {

  /**
   * Datadog recommend instantiating a client for every thread writing to the API
   */
  private def buildClient: Client =
    scaladog.Client(
      configuration.data.datadog.apiKey,
      configuration.data.datadog.appKey,
      DatadogSite.withName(configuration.data.datadog.site)
    )

  override def onInit(init: DataWriterMessage.Init): DatadogData = {
    logger.info(s"Initialising Datadog DataWriter for run: ${init.runMessage}")
    DatadogData.initialise(init)
  }

  override def onFlush(data: DatadogData): Unit = {
    logger.info(s"Flushing to Datadog")
    val requests = sendTotalStartedUsers(
      buildClient,
      data.simulation,
      data.startedUsers.asScala.toList
    ) ++ sendTotalFinishedUsers(
      buildClient,
      data.simulation,
      data.finishedUsers.asScala.toList
    ) ++ sendTotalErrors(
      buildClient,
      data.simulation,
      data.errorCounter.asScala.toList
    ) ++ sendRequestLatencySeconds(
      buildClient,
      data.simulation,
      data.requestLatency.asScala.toList
    )

    for {
      response <- requests
    } yield {
      response.onComplete {
        case Success(StatusResponse(status)) =>
          logger.info(s"Flushed data to Datadog with response: $status")
        case Failure(exception) =>
          logger.error(s"Failed to flush data to Datadog with error: ${exception.getMessage}")
      }
    }
  }

  override def onCrash(cause: String, data: DatadogData): Unit =
    logger.error(cause)

  override def onStop(data: DatadogData): Unit = {
    logger.info(s"Received Stop message")
    onFlush(data)
  }

  override def onMessage(message: LoadEvent, data: DatadogData): Unit =
    message match {
      case userStart: UserStart => onUserStartMessage(userStart, data)
      case userEnd: UserEnd     => onUserEndMessage(userEnd, data)
      case response: Response   => onResponseMessage(response, data)
      case error: EventError    => onErrorMessage(error, data)
      case _                    => ()
    }

  private def onUserStartMessage(user: UserStart, data: DatadogData): Unit = {
    logger.info(s"Received UserStart message")
    data.startedUsers.put(
      UserLabels(currentInstant, user.scenario),
      BigDecimal.valueOf(1)
    )
  }

  private def onUserEndMessage(user: UserEnd, data: DatadogData): Unit = {
    logger.info(s"Received UserEnd message")
    data.finishedUsers.put(
      UserLabels(currentInstant, user.scenario),
      BigDecimal.valueOf(1)
    )
  }

  private def onResponseMessage(response: Response, data: DatadogData): Unit = {
    logger.info(s"Received Response message")
    data.requestLatency.put(
      ResponseLabels(
        currentInstant,
        response.scenario,
        response.status.toString
      ),
      BigDecimal.valueOf(
        (response.endTimestamp - response.startTimestamp) / 1000d
      )
    )
  }

  private def onErrorMessage(error: EventError, data: DatadogData): Unit = {
    logger.info(s"Received EventError message")
    data.errorCounter.put(ErrorLabels(currentInstant), BigDecimal.valueOf(1))
  }

  private def currentInstant = Instant.ofEpochMilli(clock.nowMillis)
}

object DatadogRequests {
  def sendTotalStartedUsers(
                             client: Client,
                             simulation: String,
                             startedUsers: List[(UserLabels, BigDecimal)]
                           )(implicit
                             ec: ExecutionContext
                           ): Seq[Future[StatusResponse]] = {
    val futures = for {
      (scenario, users) <- startedUsers.groupBy(_._1.scenario)
    } yield {
      send(
        client,
        "total_started_users",
        MetricType.Count,
        users,
        Seq(
          s"simulation:$simulation",
          s"scenario:$scenario"
        )
      )
    }
    futures.toSeq
  }

  def sendTotalFinishedUsers(
                              client: Client,
                              simulation: String,
                              finishedUsers: List[(UserLabels, BigDecimal)]
                            )(implicit
                              ec: ExecutionContext
                            ): Seq[Future[StatusResponse]] = {
    val futures = for {
      (scenario, users) <- finishedUsers.groupBy(_._1.scenario)
    } yield {
      send(
        client,
        "total_finished_users",
        MetricType.Count,
        users,
        Seq(
          s"simulation:$simulation",
          s"scenario:$scenario"
        )
      )
    }
    futures.toSeq
  }

  def sendTotalErrors(
                       client: Client,
                       simulation: String,
                       errors: List[(ErrorLabels, BigDecimal)]
                     )(implicit ec: ExecutionContext): Seq[Future[StatusResponse]] =
    Seq(
      send(
        client,
        "total_errors",
        MetricType.Count,
        errors,
        Seq(
          s"simulation:$simulation"
        )
      )
    )

  def sendRequestLatencySeconds(
                                 client: Client,
                                 simulation: String,
                                 requestLatency: List[(ResponseLabels, BigDecimal)]
                               )(implicit
                                 ec: ExecutionContext
                               ): Seq[Future[StatusResponse]] = {
    val futures = for {
      (scenario, requestsByScenario) <- requestLatency.groupBy(_._1.scenario)
      (status, requestsByScenarioAndStatus) <- requestsByScenario.groupBy(
        _._1.status
      )
    } yield {
      send(
        client,
        "request_latency_seconds",
        MetricType.Gauge,
        requestsByScenarioAndStatus,
        Seq(
          s"simulation:$simulation",
          s"scenario:$scenario",
          s"status$status"
        )
      )
    }
    futures.toSeq
  }

  def send(
            client: Client,
            metricName: String,
            metricType: MetricType,
            dataPoints: Seq[(DatadogLabels, BigDecimal)],
            tags: Seq[String]
          )(implicit ec: ExecutionContext): Future[StatusResponse] =
    Future {
      blocking {
        client.metrics
          .postMetrics(
            Seq(
              Series(
                metric = metricName,
                points = dataPoints.map { case (labels: ResponseLabels, value: BigDecimal) =>
                  Point(labels.instant, value)
                },
                tags = tags,
                metricType = metricType
              )
            )
          )
      }
    }
}
