/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.ws.fsm

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.Throwables._
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.action.ws.{ OnConnectedChainEndAction, WsListener }
import io.gatling.http.cache.SslContextSupport
import io.gatling.http.check.ws.WsFrameCheckSequence
import io.gatling.http.client.WebSocket
import io.gatling.http.cookie.CookieSupport

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.HttpResponseStatus.SWITCHING_PROTOCOLS
import io.netty.handler.codec.http.cookie.Cookie

object WsConnectingState extends StrictLogging {
  private val WsConnectSuccessStatusCode = Some(Integer.toString(SWITCHING_PROTOCOLS.code))

  def gotoConnecting(fsm: WsFsm, session: Session, next: Either[Action, SendFrame]): NextWsState =
    gotoConnecting(fsm, session, next, fsm.httpProtocol.wsPart.maxReconnects)

  def gotoConnecting(fsm: WsFsm, session: Session, next: Either[Action, SendFrame], remainingTries: Int): NextWsState = {

    import fsm._

    val listener = new WsListener(fsm, clock)

    // [fl]
    //
    // [fl]
    logger.debug(s"Connecting to ${connectRequest.getUri}")
    val userSslContexts = SslContextSupport.sslContexts(session)
    httpEngine.executeRequest(
      connectRequest,
      session.userId,
      httpProtocol.enginePart.shareConnections,
      session.eventLoop,
      listener,
      userSslContexts.map(_.sslContext).orNull,
      userSslContexts.flatMap(_.alpnSslContext).orNull
    )

    NextWsState(new WsConnectingState(fsm, session, next, clock.nowMillis, remainingTries))
  }
}

final case class WsConnectingState(fsm: WsFsm, session: Session, next: Either[Action, SendFrame], connectStart: Long, remainingTries: Int)
    extends WsState(fsm)
    with StrictLogging {

  import fsm._

  override def onWebSocketConnected(webSocket: WebSocket, cookies: List[Cookie], connectEnd: Long): NextWsState = {
    val sessionWithCookies = CookieSupport.storeCookies(session, connectRequest.getUri, cookies, connectEnd)
    val sessionWithGroupTimings =
      logResponse(sessionWithCookies, connectActionName, connectStart, connectEnd, OK, WsConnectingState.WsConnectSuccessStatusCode, None)

    connectCheckSequence match {
      case WsFrameCheckSequence(timeout, currentCheck :: remainingChecks) :: remainingCheckSequences =>
        // wait for some checks before proceeding

        // select nextAction
        val (newSession, newNext) =
          onConnected match {
            case Some(onConnectedAction) =>
              // once check complete, perform connect sequence
              // we store the after next action in the session
              // other solution would be to store in FSM state but we'd need one more message passing to contact the actor from ConnectSequenceEndAction

              val onConnectedChainEndCallback: Session => Unit = next match {
                case Left(nextAction) =>
                  logger.debug("Connected, performing checks, setting callback to perform next action after performing onConnected action")
                  nextAction ! _

                case Right(sendFrame) =>
                  logger.debug("Connected, performing checks, setting callback to send pending message after performing onConnected action")
                  sendFrameNextAction(_, sendFrame)
              }

              (OnConnectedChainEndAction.setOnConnectedChainEndCallback(sessionWithGroupTimings, onConnectedChainEndCallback), Left(onConnectedAction))

            case _ =>
              // once check complete -> send next
              logger.debug("Connected, performing checks before proceeding (no onConnected action)")
              (sessionWithGroupTimings, next)
          }

        fsm.scheduleTimeout(timeout)
        //[fl]
        //
        //[fl]

        NextWsState(
          WsPerformingCheckState(
            fsm,
            webSocket = webSocket,
            currentCheck = currentCheck,
            remainingChecks = remainingChecks,
            checkSequenceStart = connectEnd,
            remainingCheckSequences = remainingCheckSequences,
            session = newSession,
            next = newNext
          )
        )

      case _ => // same as Nil as WsFrameCheckSequence#checks can't be Nil, but compiler complains that match may not be exhaustive
        onConnected match {
          case Some(onConnectedAction) =>
            // connect sequence -> store actual next
            val onConnectedChainEndCallback: Session => Unit = next match {
              case Left(nextAction) =>
                logger.debug("Connected, no checks, performing onConnected action before performing next action")
                nextAction ! _

              case Right(sendFrame) =>
                logger.debug("Reconnected, no checks, performing onConnected action before sending pending message")
                sendFrameNextAction(_, sendFrame)
            }
            val newSession = OnConnectedChainEndAction.setOnConnectedChainEndCallback(sessionWithGroupTimings, onConnectedChainEndCallback)

            NextWsState(
              new WsIdleState(fsm, newSession, webSocket),
              () => onConnectedAction ! newSession
            )

          case _ =>
            val afterStateUpdate =
              next match {
                case Left(nextAction) =>
                  logger.debug("Connected, no checks, performing next action")
                  () => nextAction ! sessionWithGroupTimings

                case Right(sendFrame) =>
                  logger.debug("Reconnected, no checks, sending pending message")
                  sendFrameNextAction(sessionWithGroupTimings, sendFrame)
              }

            NextWsState(
              new WsIdleState(fsm, sessionWithGroupTimings, webSocket),
              afterStateUpdate
            )
        }
    }
  }

  override def onWebSocketCrashed(t: Throwable, timestamp: Long): NextWsState = {
    // crash
    logger.debug(s"WebSocket crashed by the server while in Connecting state", t)
    val failedSession = session.markAsFailed
    logResponse(failedSession, connectActionName, connectStart, timestamp, KO, Some(t.rootMessage), None)

    val n = next match {
      case Left(nextAction) => nextAction
      case Right(sendFrame) => sendFrame.next
    }
    logger.debug("Connect failed, performing next action")

    NextWsState(new WsCrashedState(fsm, Some(t.rootMessage)), () => n ! failedSession)
  }
}
