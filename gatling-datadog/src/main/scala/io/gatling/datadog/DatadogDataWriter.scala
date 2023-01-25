package io.gatling.datadog

import java.time.Instant
import java.util.UUID

import io.gatling.commons.util.Clock
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.writer.{DataWriter, DataWriterMessage}
import io.gatling.core.stats.writer.DataWriterMessage.LoadEvent
import io.gatling.core.stats.writer.DataWriterMessage.LoadEvent.{
  Response,
  UserEnd,
  UserStart,
  Error => EventError
}
import io.gatling.core.util.NameGen
import akka.actor.ActorRef

private[gatling] class DatadogDataWriter(
    clock: Clock,
    configuration: GatlingConfiguration
) extends DataWriter[DatadogData]
    with NameGen {

  private val flushTimerName = "DatadogFlushTimer"

  override def onInit(init: DataWriterMessage.Init): DatadogData = {
    logger.info(
      s"Initializing Datadog DataWriter for run: ${init.runMessage.runId}"
    )
    val metricsSender: ActorRef = context.actorOf(
      MetricsSender.props(configuration)(dispatcher, logger),
      genName("metricsSender")
    )
    startTimerAtFixedRate(
      flushTimerName,
      DataWriterMessage.Flush,
      configuration.data.datadog.writePeriod
    )
    DatadogData.initialise(metricsSender, init)
  }

  override def onFlush(data: DatadogData): Unit = {
    logger.info(s"Flushing to Datadog for run ${data.runId}")
    sendMetricsToDatadog(data)

  }

  override def onCrash(cause: String, data: DatadogData): Unit =
    logger.error(cause)

  override def onStop(data: DatadogData): Unit = {
    cancelTimer(flushTimerName)
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

  private def onUserStartMessage(user: UserStart, data: DatadogData): Unit =
    data.startedUsers.put(
      UUID.randomUUID(),
      UserLabels(currentInstant, user.scenario, BigDecimal.valueOf(1))
    )

  private def onUserEndMessage(user: UserEnd, data: DatadogData): Unit =
    data.finishedUsers.put(
      UUID.randomUUID(),
      UserLabels(currentInstant, user.scenario, BigDecimal.valueOf(1))
    )

  private def onResponseMessage(response: Response, data: DatadogData): Unit =
    data.requestLatency.put(
      UUID.randomUUID(),
      ResponseLabels(
        currentInstant,
        response.scenario,
        response.status.toString,
        BigDecimal.valueOf(
          response.endTimestamp - response.startTimestamp
        )
      )
    )

  private def onErrorMessage(error: EventError, data: DatadogData): Unit =
    data.errorCounter.put(
      UUID.randomUUID(),
      ErrorLabels(currentInstant, BigDecimal.valueOf(1))
    )

  private def currentInstant = Instant.ofEpochMilli(clock.nowMillis)

  private def sendMetricsToDatadog(data: DatadogData): Unit = {
    data.metricsSender ! data
  }
}
