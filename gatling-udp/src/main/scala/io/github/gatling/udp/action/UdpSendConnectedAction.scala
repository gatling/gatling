package io.github.gatling.udp.action

import com.typesafe.scalalogging.StrictLogging
import io.gatling.commons.stats.{KO, OK, Status}
import io.gatling.commons.util.Clock
import io.gatling.commons.validation.Validation
import io.gatling.core.action.{Action, RequestAction}
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.github.gatling.udp.protocol.UdpComponents
import io.github.gatling.udp.request.{UdpAttributes, UdpMessage}

import java.net.DatagramPacket

case class UdpSendConnectedAction(
    attributes: UdpAttributes,
    udpComponents: UdpComponents,
    val next: Action,
    message: UdpMessage,
    throttler: Option[Throttler]
) extends RequestAction with NameGen with StrictLogging {

  override val requestName: Expression[String] = attributes.requestName

  override val name: String = genName("UdpSendConnected")

  override def statsEngine: StatsEngine = udpComponents.coreComponents.statsEngine

  override def clock: Clock = udpComponents.coreComponents.clock

  override def sendRequest(session: Session): Validation[Unit] =
    for {
      reqName <- requestName(session)
      message <- message.udpMessage(session)
    } yield {
      logger.trace(s"sendRequest message ${message}")
      def sendMessage =
        send(
          reqName,
          message,
          udpComponents,
          session,
        )
      throttler match {
        case Some(th) => th.throttle(session.scenario, () => sendMessage)
        case _ => sendMessage
      }
    }

  protected def send(
                      requestName: String,
                      message: Array[Byte],
                      udpComponents: UdpComponents,
                      session: Session,
                    ): Unit = {
    logger.trace(s"send message ${message}")
    val sendStartDate = clock.nowMillis
    var status: Status = OK
    var messageResponse: Option[String] = None
    try {
      udpComponents.udpConnection.send(
        new DatagramPacket(
          message,
          message.length,
        ))
    } catch {
      case e: Throwable =>
        status = KO
        messageResponse = Some(e.getMessage)
    }

    statsEngine.logResponse(
      scenario = session.scenario,
      groups = session.groups,
      requestName,
      startTimestamp = sendStartDate,
      endTimestamp = clock.nowMillis,
      status,
      None,
      messageResponse,
    )
    if (status == OK) {
      logger.trace(s"UdpSendConnectedAction status ${status} markAsSucceeded")
      next ! session.markAsSucceeded
    }
    else {
      logger.trace(s"UdpSendConnectedAction status ${status} markAsFailed")
      next ! session.markAsFailed
    }
  }
}