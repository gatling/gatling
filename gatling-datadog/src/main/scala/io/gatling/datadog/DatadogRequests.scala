package io.gatling.datadog

import java.util.UUID

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.jdk.CollectionConverters.{ConcurrentMapHasAsScala, EnumerationHasAsScala}
import scala.util.{Failure, Success}

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.Logger
import io.gatling.core.config.GatlingConfiguration
import io.gatling.datadog.DatadogRequests.sendMetrics
import scaladog.Client
import scaladog.api.{DatadogSite, StatusResponse}
import scaladog.api.metrics.{MetricType, Point, Series}

object DatadogRequests {
  def sendTotalStartedUsers(
      client: Client,
      batchSize: Int,
      runId: String,
      simulation: String,
      startedUsers: List[(UUID, UserLabels)]
  )(implicit
      ec: ExecutionContext
  ): Seq[Future[StatusResponse]] = {
    val futures = for {
      (scenario, users) <- startedUsers.groupBy(_._2.scenario)
      batchedUsers <- users.grouped(batchSize).toSeq
    } yield {
      send(
        client,
        "gatling.performance_tests.total_started_users",
        MetricType.Gauge,
        batchedUsers,
        Seq(
          s"run:$runId",
          s"simulation:$simulation",
          s"scenario:$scenario"
        )
      )
    }
    futures.toSeq
  }

  def sendTotalFinishedUsers(
      client: Client,
      batchSize: Int,
      runId: String,
      simulation: String,
      finishedUsers: List[(UUID, UserLabels)]
  )(implicit
      ec: ExecutionContext
  ): Seq[Future[StatusResponse]] = {
    val futures = for {
      (scenario, users) <- finishedUsers.groupBy(_._2.scenario)
      batchedUsers <- users.grouped(batchSize).toSeq
    } yield {
      send(
        client,
        "gatling.performance_tests.total_finished_users",
        MetricType.Gauge,
        batchedUsers,
        Seq(
          s"run:$runId",
          s"simulation:$simulation",
          s"scenario:$scenario"
        )
      )
    }
    futures.toSeq
  }

  def sendTotalErrors(
      client: Client,
      batchSize: Int,
      runId: String,
      simulation: String,
      errors: List[(UUID, ErrorLabels)]
  )(implicit ec: ExecutionContext): Seq[Future[StatusResponse]] = {
    val futures = for {
      batchedErrors <- errors.grouped(batchSize).toSeq
    } yield {
      send(
        client,
        "gatling.performance_tests.total_errors",
        MetricType.Gauge,
        batchedErrors,
        Seq(
          s"run:$runId",
          s"simulation:$simulation"
        )
      )
    }
    futures
  }

  def sendRequestLatencyMillis(
      client: Client,
      batchSize: Int,
      runId: String,
      simulation: String,
      requestLatency: List[(UUID, ResponseLabels)]
  )(implicit
      ec: ExecutionContext
  ): Seq[Future[StatusResponse]] = {
    val futures = for {
      (scenario, requestsByScenario) <- requestLatency.groupBy(_._2.scenario)
      (status, requestsByScenarioAndStatus) <- requestsByScenario.groupBy(
        _._2.status
      )
      batchedRequests <- requestsByScenarioAndStatus.grouped(batchSize).toSeq
    } yield {
      send(
        client,
        "gatling.performance_tests.request_latency_millis",
        MetricType.Gauge,
        batchedRequests,
        Seq(
          s"run:$runId",
          s"simulation:$simulation",
          s"scenario:$scenario",
          s"status$status"
        )
      )
    }
    futures.toSeq
  }

  def sendMetrics(client: Client, batchSize: Int, data: DatadogData)(implicit
      ec: ExecutionContext,
      logger: Logger
  ): Unit = {
    val startedUsers = data.startedUsers
    val startedUsersRequests = sendTotalStartedUsers(
      client,
      batchSize,
      data.runId,
      data.simulation,
      startedUsers.asScala.toList
    )

    val finishedUsers = data.finishedUsers
    val finishedUsersRequests = sendTotalFinishedUsers(
      client,
      batchSize,
      data.runId,
      data.simulation,
      finishedUsers.asScala.toList
    )

    val requestLatency = data.requestLatency
    val requestLatencyRequests = sendRequestLatencyMillis(
      client,
      batchSize,
      data.runId,
      data.simulation,
      requestLatency.asScala.toList
    )

    val errorCounter = data.errorCounter
    val errorCounterRequests = sendTotalErrors(
      client,
      batchSize,
      data.runId,
      data.simulation,
      errorCounter.asScala.toList
    )

    startedUsersRequests.foreach { startedUsersRequest =>
      startedUsersRequest.onComplete {
        case Success(StatusResponse(status)) =>
          startedUsers
            .keys()
            .asScala
            .toSeq
            .foreach(id => DatadogData.remove(id, startedUsers))
        case Failure(e) =>
          logger.error(s"Started users request failed with error $e")
      }
    }

    finishedUsersRequests.foreach { finishedUsersRequest =>
      finishedUsersRequest.onComplete {
        case Success(StatusResponse(status)) =>
          finishedUsers
            .keys()
            .asScala
            .toSeq
            .foreach(id => DatadogData.remove(id, finishedUsers))
        case Failure(e) =>
          logger.error(s"Finished users request failed with error $e")
      }
    }

    requestLatencyRequests.foreach { requestLatencyRequest =>
      requestLatencyRequest.onComplete {
        case Success(StatusResponse(status)) =>
          requestLatency
            .keys()
            .asScala
            .toSeq
            .foreach(id => DatadogData.remove(id, requestLatency))
        case Failure(e) =>
          logger.error(s"Request latency request failed with error $e")
      }
    }

    errorCounterRequests.foreach { errorCounterRequest =>
      errorCounterRequest.onComplete {
        case Success(StatusResponse(status)) =>
          errorCounter
            .keys()
            .asScala
            .toSeq
            .foreach(id => DatadogData.remove(id, errorCounter))
        case Failure(e) =>
          logger.error(s"Error count request failed with error $e")
      }
    }
  }

  def send(
      client: Client,
      metricName: String,
      metricType: MetricType,
      dataPoints: Seq[(UUID, DatadogLabels)],
      tags: Seq[String]
  )(implicit ec: ExecutionContext): Future[StatusResponse] =
    Future {
      blocking {
        client.metrics
          .postMetrics(
            Seq(
              Series(
                metric = metricName,
                points = dataPoints.map { case (_, labels: DatadogLabels) =>
                  Point(labels.instant, labels.value)
                },
                tags = tags,
                metricType = metricType
              )
            )
          )
      }
    }
}

private[datadog] object MetricsSender {
  def props(
      configuration: GatlingConfiguration
  )(implicit ec: ExecutionContext, logger: Logger): Props = {
    val apiKey = configuration.data.datadog.apiKey
    val appKey = configuration.data.datadog.appKey
    val site = configuration.data.datadog.site
    val bufferSize = configuration.data.datadog.bufferSize
    Props(
      new DatadogApiSender(apiKey, appKey, site, bufferSize)
    )
  }
}

private[datadog] class DatadogApiSender(
    apiKey: String,
    appKey: String,
    site: String,
    bufferSize: Int
)(implicit ec: ExecutionContext,
  logger: Logger) extends Actor {
  val client = scaladog.Client(
    apiKey,
    appKey,
    DatadogSite.withName(site)
  )

  def receive: Receive = { case data: DatadogData =>
    logger.info(
      s"Received request to push data to Datadog for run: ${data.runId}"
    )
    sendMetrics(client, bufferSize, data)
  }
}
