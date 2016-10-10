/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.ws2

import java.util.{ ArrayList => JArrayList }

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.commons.validation.{ Failure, Success }
import io.gatling.core.action.Action
import io.gatling.core.check.Check
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.protocol.HttpProtocol

import akka.actor.Props
import io.netty.handler.codec.http.HttpResponseStatus.SWITCHING_PROTOCOLS
import org.asynchttpclient.Request
import org.asynchttpclient.ws.{ WebSocket, WebSocketListener, WebSocketUpgradeHandler }

case class PerformInitialConnect(session: Session, initialConnectNext: Action)
case class WebSocketOpened(websocket: WebSocket, timestamp: Long)
case class SendTextMessage(actionName: String, message: String, checkSequences: List[WsCheckSequence], session: Session, next: Action)
case class TextMessageReceived(message: String, timestamp: Long)
case class WebSocketClosed(code: Int, reason: String, timestamp: Long)
case class WebSocketCrashed(t: Throwable, timestamp: Long)
case class ClientCloseRequest(actionName: String, session: Session, next: Action)
case class Timeout(id: Long)

object WsActor {

  val WsConnectSuccessStatusCode = Some(Integer.toString(SWITCHING_PROTOCOLS.code))

  val TimeoutTimerName = "timeout"

  def props(
    wsName:               String,
    connectRequest:       Request,
    connectActionName:    String,
    connectCheckSequence: List[WsCheckSequence],
    onConnected:          Option[Action],
    statsEngine:          StatsEngine,
    httpEngine:           HttpEngine,
    httpProtocol:         HttpProtocol,
    configuration:        GatlingConfiguration
  ) =
    Props(new WsActor(
      wsName,
      connectRequest,
      connectActionName,
      connectCheckSequence,
      onConnected,
      statsEngine,
      httpEngine,
      httpProtocol,
      configuration
    ))
}

class WsActor(
    wsName:               String,
    connectRequest:       Request,
    connectActionName:    String,
    connectCheckSequence: List[WsCheckSequence],
    onConnected:          Option[Action],
    statsEngine:          StatsEngine,
    httpEngine:           HttpEngine,
    httpProtocol:         HttpProtocol,
    configuration:        GatlingConfiguration
) extends WsActorFSM {

  private var _timeoutId = 0L
  private def scheduleTimeout(dur: FiniteDuration): Long = {
    val curr = _timeoutId
    setTimer(WsActor.TimeoutTimerName, Timeout(curr), dur, repeat = false)
    _timeoutId += 1
    curr
  }

  private def cancelTimeout(): Unit =
    cancelTimer(WsActor.TimeoutTimerName)

  // FIXME we have a max number of consecutive retries instead?
  private val maxConsecutiveReconnects = httpProtocol.wsPart.maxReconnects.getOrElse(0)
  private var _remainingReconnects = maxConsecutiveReconnects
  private def getAndDecrementRemainingReconnects(): Int = {
    val curr = _remainingReconnects
    _remainingReconnects -= 1
    curr
  }
  private def resetReconnects(): Unit = {
    _remainingReconnects = maxConsecutiveReconnects
  }

  //[fl]
  //
  //
  //[fl]

  private def logResponse(session: Session, actionName: String, start: Long, end: Long, status: Status, code: Option[String], reason: Option[String]): Session = {
    val timings = ResponseTimings(start, end)
    val newSession = session.logGroupRequest(timings.responseTime, status)
    statsEngine.logResponse(newSession, actionName, timings, status, code, reason, Nil)
    newSession
  }

  private def logUnmatchedServerMessage(session: Session): Unit =
    statsEngine.logResponse(session, wsName, ResponseTimings(nowMillis, Long.MinValue), OK, None, None)

  startWith(Init, InitData)

  private def connect(session: Session, next: Action): State = {
    val (newSession, client) = httpEngine.httpClient(session, httpProtocol)

    val handler = {
      // can't use a singletonList as list will be cleared on close
      val listeners = new JArrayList[WebSocketListener](1)
      listeners.add(new WsListener(self))
      configuration.resolve(
        // [fl]
        //
        //
        //
        // [fl]
        new WebSocketUpgradeHandler(listeners)
      )
    }

    // [fl]
    //
    // [fl]
    client.executeRequest(connectRequest, handler)

    goto(Connecting) using ConnectingData(newSession, next, nowMillis)
  }

  when(Init) {
    case Event(PerformInitialConnect(session, initialConnectNext), InitData) =>
      connect(session.set(wsName, self), initialConnectNext)
  }

  private def handleConnectFailure(session: Session, next: Action, connectStart: Long, connectEnd: Long, code: Option[String], reason: String): State = {
    // log connect failure
    val newSession = logResponse(session, connectActionName, connectStart, connectEnd, KO, code, Some(reason))
    if (getAndDecrementRemainingReconnects() > 0) {
      // try again
      connect(newSession, next)

    } else {
      // failed to connect
      resetReconnects()
      next ! newSession.markAsFailed
      goto(Crashed) using CrashedData(Some(reason))
    }
  }

  when(Connecting) {

    case Event(WebSocketOpened(webSocket, connectEnd), ConnectingData(session, next, connectStart)) =>
      resetReconnects()
      val sessionWithGroupTimings = logResponse(session, connectActionName, connectStart, connectEnd, OK, WsActor.WsConnectSuccessStatusCode, None)

      connectCheckSequence match {
        case Nil =>
          onConnected match {
            case None =>
              // send next
              next ! sessionWithGroupTimings
            case Some(sequence) =>
              // connect sequence -> send actual next
              sequence ! sessionWithGroupTimings.set(OnConnectedChainEndAction.NextAction, next)
          }

          goto(Idle) using IdleData(sessionWithGroupTimings, webSocket)

        case firstCheckSequence :: remainingCheckSequences =>
          // wait for some checks before proceeding

          // select nextAction
          val (newSession, nextAction) =
            onConnected match {
              case None =>
                // check complete -> send next
                (sessionWithGroupTimings, next)
              case Some(action) =>
                // check complete -> connect sequence -> send actual next
                // we store the after next action in the session
                // other solution would be to store in FSM state but we'd need one more message passing to contact the actor from ConnectSequenceEndAction
                (sessionWithGroupTimings.set(OnConnectedChainEndAction.NextAction, next), action)
            }

          val timeoutId = scheduleTimeout(firstCheckSequence.timeout)
          //[fl]
          //
          //[fl]
          goto(PerformingCheck) using PerformingCheckData(
            websocket = webSocket,
            ongoingChecks = firstCheckSequence.checks,
            checkSequenceStart = connectEnd,
            checkSequenceTimeoutId = timeoutId,
            remainingCheckSequences = remainingCheckSequences,
            session = newSession,
            next = nextAction
          )
      }

    case Event(WebSocketClosed(code, reason, timestamp), ConnectingData(session, next, connectStart)) =>
      // unexpected close
      handleConnectFailure(session, next, connectStart, timestamp, Some(String.valueOf(code)), reason)

    case Event(WebSocketCrashed(t, timestamp), ConnectingData(session, next, connectStart)) =>
      // crash
      handleConnectFailure(session, next, connectStart, timestamp, None, t.getMessage)
  }

  private def handleWebSocketCheckCrash(actionName: String, session: Session, next: Action, checkSequenceStart: Long, code: Option[String], errorMessage: String): State = {
    val fullMessage = s"WebSocket crashed while waiting for check: $errorMessage"

    val newSession = logResponse(session, actionName, checkSequenceStart, nowMillis, KO, code, Some(fullMessage))
    next ! newSession
    goto(Crashed) using CrashedData(Some(errorMessage))
  }

  when(PerformingCheck) {

    case Event(Timeout(timeoutId), PerformingCheckData(webSocket, currentCheck :: _, checkSequenceStart, currentTimeoutId, _, session, next)) =>
      if (timeoutId == currentTimeoutId) {
        logger.debug(s"Check timeout $timeoutId")
        // check timeout
        // fail check, send next and goto Idle
        // FIXME should we fail all the other blocking checks too?
        val errorMessage = s"Check ${currentCheck.name} timeout"
        val newSession = logResponse(session, currentCheck.name, checkSequenceStart, nowMillis, KO, None, Some(errorMessage))
        next ! newSession
        goto(Idle) using IdleData(newSession, webSocket)
      } else {
        logger.debug(s"Out-of-band timeout $timeoutId")
        // out-of-band timeoutId, ignore
        stay()
      }

    case Event(TextMessageReceived(message, timestamp), data @ PerformingCheckData(webSocket, currentCheck :: remainingChecks, checkSequenceStart, currentTimeoutId, remainingCheckSequences, session, next)) =>

      // cache is used for both matching and checking
      implicit val cache = collection.mutable.HashMap.empty[Any, Any]

      // messages match by default
      val messageMatches = currentCheck.matchConditions.forall(_.check(message, session).isInstanceOf[Success[_]])

      if (messageMatches) {
        logger.debug(s"Received matching message $message")
        cancelTimeout() // note, we might already have a Timeout in the mailbox, hence the currentTimeoutId check
        // matching message, apply checks
        val (checkSaveUpdate, checkError) = Check.check(message, session, currentCheck.checks)

        val sessionWithCheckUpdate = checkSaveUpdate(session)

        checkError match {
          case None =>
            logger.debug("Current check success")
            // check success
            val newSession = logResponse(sessionWithCheckUpdate, currentCheck.name, checkSequenceStart, nowMillis, OK, None, None)
            remainingChecks match {
              case Nil =>

                remainingCheckSequences match {
                  case Nil =>
                    // all check sequences complete
                    logger.debug("Check sequences completed successfully")
                    next ! newSession
                    goto(Idle) using IdleData(newSession, webSocket)

                  case nextCheckSequence :: nextRemainingCheckSequences =>
                    logger.debug("Perform next check sequence")
                    // perform next CheckSequence
                    val timeoutId = scheduleTimeout(nextCheckSequence.timeout)
                    //[fl]
                    //
                    //[fl]
                    stay() using data.copy(ongoingChecks = nextCheckSequence.checks, checkSequenceStart = nowMillis, checkSequenceTimeoutId = timeoutId, remainingCheckSequences = nextRemainingCheckSequences, session = newSession)
                }

              case nextCheck :: _ =>
                // perform next check
                logger.debug("Perform next check")
                //[fl]
                //
                //[fl]
                stay() using data.copy(ongoingChecks = remainingChecks, session = newSession)
            }

          case Some(Failure(errorMessage)) =>
            logger.debug("Check failure")
            val newSession = logResponse(sessionWithCheckUpdate, currentCheck.name, checkSequenceStart, nowMillis, KO, None, Some(errorMessage))
            // FIXME should we fail all the other blocking checks immediately?
            next ! newSession
            goto(Idle) using IdleData(newSession, webSocket)
        }
      } else {
        logger.debug(s"Received non-matching message $message")
        // server unmatched message, just log
        logUnmatchedServerMessage(session)
        stay()
      }

    case Event(WebSocketClosed(code, reason, timestamp), PerformingCheckData(webSocket, currentCheck :: _, checkSequenceStart, _, _, session, next)) =>
      // unexpected close, fail check
      logger.debug("WebSocket remotely closed while waiting for checks")
      cancelTimeout()
      handleWebSocketCheckCrash(currentCheck.name, session, next, checkSequenceStart, Some(Integer.toString(code)), reason)

    case Event(WebSocketCrashed(t, timestamp), PerformingCheckData(webSocket, currentCheck :: _, checkSequenceStart, _, _, session, next)) =>
      // crash, fail check
      logger.debug("WebSocket crashed while waiting for checks")
      cancelTimeout()
      handleWebSocketCheckCrash(currentCheck.name, session, next, checkSequenceStart, None, t.getMessage)
  }

  when(Idle) {
    case Event(SendTextMessage(actionName, message, checkSequences, session, next), IdleData(_, websocket)) =>
      logger.debug(s"Send message $actionName $message")
      // actually send message!
      val timestamp = nowMillis
      websocket.sendMessage(message)

      //[fl]
      //
      //[fl]
      checkSequences match {
        case Nil =>
          stay()

        case firstCheckSequence :: remainingCheckSequences =>
          logger.debug("Trigger check after send message")
          val timeoutId = scheduleTimeout(firstCheckSequence.timeout)
          //[fl]
          //
          //[fl]
          goto(PerformingCheck) using PerformingCheckData(
            websocket = websocket,
            ongoingChecks = firstCheckSequence.checks,
            checkSequenceStart = timestamp,
            checkSequenceTimeoutId = timeoutId,
            remainingCheckSequences: List[WsCheckSequence],
            session = session,
            next = next
          )
      }

    case Event(TextMessageReceived(message, timestamp), IdleData(session, _)) =>
      // server push message, just log
      logger.debug(s"Received push message $message")
      logUnmatchedServerMessage(session)
      stay()

    case Event(WebSocketClosed(code, reason, timestamp), _) =>
      // server issued close
      logger.info("WebSocket was forcefully closed by the server while in Idle state")
      goto(Crashed) using CrashedData(None)

    case Event(WebSocketCrashed(t, timestamp), _) =>
      // crash
      logger.info("WebSocket crashed by the server while in Idle state", t)
      goto(Crashed) using CrashedData(Some(t.getMessage))

    case Event(ClientCloseRequest(name, session, next), IdleData(_, websocket)) =>
      logger.info("Client requested WebSocket close")
      websocket.close()
      //[fl]
      //
      //[fl]
      goto(Closing) using ClosingData(name, session, next, nowMillis) // TODO should we have a close timeout?
  }

  when(Closing) {

    case Event(TextMessageReceived(message, timestamp), ClosingData(_, session, _, _)) =>
      logUnmatchedServerMessage(session)
      stay()

    case Event(WebSocketClosed(code, reason, timestamp), ClosingData(actionName, session, next, closeStart)) =>
      // server has acked closing
      logger.info("Server has acked closing")
      val newSession = logResponse(session, actionName, closeStart, timestamp, OK, None, None)
      next ! newSession.remove(wsName)
      stop()

    case Event(WebSocketCrashed(t, timestamp), ClosingData(actionName, session, next, closeStart)) =>
      logger.info("WebSocket crashed while waiting for close ack")
      // crash, close anyway
      val newSession = logResponse(session, actionName, closeStart, timestamp, KO, None, Some(t.getMessage))
      next ! newSession.markAsFailed.remove(wsName)
      stop()
  }

  when(Crashed) {
    case Event(ClientCloseRequest(actionName, session, next), CrashedData(errorMessage)) =>
      val newSession = errorMessage match {
        case None =>
          logger.info("Client issued close order but WebSocket was already ${ if (graceful) closed")
          session
        case Some(mess) =>
          val newSession = session.markAsFailed
          statsEngine.logCrash(newSession, actionName, s"Client issued close order but WebSocket was already crashed: $mess")
          newSession
      }

      next ! newSession.remove(wsName)
      stop()

    case Event(SendTextMessage(actionName, message, _, session, next), CrashedData(errorMessage)) =>
      val message = errorMessage match {
        case None       => "Client issued message but WebSocket was already closed"
        case Some(mess) => s"Client issued message but WebSocket was already crashed: $mess"
      }

      logger.info(message)
      val newSession = session.markAsFailed
      statsEngine.logCrash(newSession, actionName, message)

      // perform blocking reconnect
      connect(newSession, next)
  }

  // TODO unhandled

  initialize()
}
