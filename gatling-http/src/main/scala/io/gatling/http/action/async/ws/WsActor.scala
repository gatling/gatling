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
package io.gatling.http.action.async.ws

import scala.collection.mutable

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.commons.validation.Success
import io.gatling.core.stats.StatsEngine
import io.gatling.http.action.async._
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.check.async._

import akka.actor.Props
import org.asynchttpclient.ws.WebSocket

object WsActor {
  def props(wsName: String, statsEngine: StatsEngine, httpEngine: HttpEngine) =
    Props(new WsActor(wsName, statsEngine, httpEngine))
}

class WsActor(wsName: String, statsEngine: StatsEngine, httpEngine: HttpEngine) extends AsyncProtocolActor(statsEngine) {

  private def goToOpenState(webSocket: WebSocket): NextTxBasedBehaviour =
    tx => openState(webSocket, tx)

  def receive = initialState

  val initialState: Receive = {

    case OnOpen(tx, webSocket, time) =>
      import tx._
      logger.debug(s"Websocket '$wsName' open")
      val newSession = session.set(wsName, self)
      val newTx = tx.copy(session = newSession)

      check match {
        case None =>
          logResponse(session, requestName, OK, start, time)
          context.become(openState(webSocket, newTx))
          next ! newSession

        case Some(c) =>
          // hack, reset check so that there's no pending one
          setCheck(newTx.copy(check = None), requestName, c, next, newSession, goToOpenState(webSocket))
      }

    case OnFailedOpen(tx, message, end) =>
      import tx._
      logger.debug(s"Websocket '$wsName' failed to open: $message")
      logResponse(session, requestName, KO, start, end, Some(message))
      next ! session.markAsFailed

      context.stop(self)
  }

  def openState(webSocket: WebSocket, tx: AsyncTx): Receive = {

      def handleClose(status: Int, reason: String, time: Long): Unit = {
        if (tx.protocol.wsPart.reconnect)
          if (tx.protocol.wsPart.maxReconnects.exists(_ <= tx.reconnectCount))
            handleCrash(s"Websocket '$wsName' was unexpectedly closed with status $status and message $reason and max reconnect was reached", time)
          else
            disconnectedState(status, reason, tx)

        else
          handleCrash(s"Websocket '$wsName' was unexpectedly closed with status $status and message $reason", time)
      }

      def handleCrash(message: String, time: Long): Unit = {

        tx.check.foreach { check =>
          logResponse(tx.session, tx.requestName, KO, tx.start, time, Some(message))
        }

        context.become(crashedState(tx, message))
      }

    {
      case Send(requestName, message, check, next, session) =>
        logger.debug(s"Sending message check on WebSocket '$wsName': $message")

        val now = nowMillis

        check match {
          case Some(c) =>
            // do this immediately instead of self sending a Listen message
            // so that other messages don't get a chance to be handled before
            setCheck(tx, requestName + " Check", c, next, session, goToOpenState(webSocket))
          case _ => reconciliate(tx, next, session, goToOpenState(webSocket))
        }

        message match {
          case TextMessage(text)    => webSocket.sendMessage(text)
          case BinaryMessage(bytes) => webSocket.sendMessage(bytes)
        }

        logResponse(session, requestName, OK, now, now)

      case SetCheck(requestName, check, next, session) =>
        logger.debug(s"Setting check on WebSocket '$wsName'")
        setCheck(tx, requestName, check, next, session, goToOpenState(webSocket))

      case CancelCheck(requestName, next, session) =>
        logger.debug(s"Cancelling check on WebSocket '$wsName'")

        val newTx = tx
          .applyUpdates(session)
          .copy(check = None, pendingCheckSuccesses = Nil)

        context.become(openState(webSocket, newTx))
        next ! newTx.session

      case CheckTimeout(check) =>
        logger.debug(s"Check on WebSocket '$wsName' timed out")

        tx.check match {
          case Some(`check`) =>
            check.expectation match {
              case ExpectedCount(count) if count == tx.pendingCheckSuccesses.size =>
                succeedPendingCheck(tx, tx.pendingCheckSuccesses, goToOpenState(webSocket))
              case ExpectedRange(range) if range.contains(tx.pendingCheckSuccesses.size) =>
                succeedPendingCheck(tx, tx.pendingCheckSuccesses, goToOpenState(webSocket))
              case _ =>
                val newTx = failPendingCheck(tx, "Check failed: Timeout")
                context.become(openState(webSocket, newTx))

                if (check.blocking)
                  // release blocked session
                  newTx.next ! newTx.applyUpdates(newTx.session).session
            }

          case _ =>
          // ignore outdated timeout
        }

      case OnTextMessage(message, time) =>
        logger.debug(s"Received text message on websocket '$wsName':$message")

        tx.check.foreach { check =>

          implicit val cache = mutable.Map.empty[Any, Any]

          check.check(message, tx.session) match {
            case Success(result) =>
              val results = result :: tx.pendingCheckSuccesses

              check.expectation match {
                case UntilCount(count) if count == results.length =>
                  succeedPendingCheck(tx, results, goToOpenState(webSocket))

                case _ =>
                  // let's pile up
                  val newTx = tx.copy(pendingCheckSuccesses = results)
                  context.become(openState(webSocket, newTx))
              }

            case _ =>
          }
        }

      case OnByteMessage(message, time) =>
        logger.debug(s"Received byte message on websocket '$wsName':$message. Beware, byte message checks are currently not supported")

      case Reconciliate(requestName, next, session) =>
        logger.debug(s"Reconciliating websocket '$wsName'")
        reconciliate(tx, next, session, goToOpenState(webSocket))

      case Close(requestName, next, session) =>
        logger.debug(s"Closing websocket '$wsName'")

        webSocket.close()

        val newTx = failPendingCheck(tx, "Check didn't succeed by the time the websocket was asked to closed")
          .applyUpdates(session)
          .copy(requestName = requestName, start = nowMillis, next = next)

        context.become(closingState(newTx))

      case OnClose(status, reason, time) =>
        logger.debug(s"Websocket '$wsName' closed by the server")
        // this close order wasn't triggered by the client, otherwise, we would have received a Close first and state would be closing or stopped

        // FIXME what about pending checks?
        handleClose(status, reason, time)

      case unexpected =>
        logger.info(s"Discarding unknown message $unexpected while in open state")
    }
  }

  def closingState(tx: AsyncTx): Receive = {
    case m: OnClose =>
      import tx._
      logResponse(session, requestName, OK, start, nowMillis)
      next ! session.remove(wsName)
      context.stop(self)

    case unexpected =>
      logger.info(s"Discarding unknown message $unexpected while in closing state")
  }

  def disconnectedState(status: Int, reason: String, tx: AsyncTx): Receive = {

    case action: WsUserAction =>
      // reconnect on first client message tentative
      val newTx = tx.copy(reconnectCount = tx.reconnectCount + 1)
      WsTx.start(newTx, self, httpEngine, statsEngine)

      context.become(reconnectingState(status, reason, action))

    case unexpected =>
      // FIXME we're losing check timeout!
      logger.info(s"Discarding unknown message $unexpected while in disconnected state")
  }

  def reconnectingState(status: Int, reason: String, pendingAction: WsUserAction): Receive = {

    case OnOpen(tx, webSocket, _) =>
      context.become(openState(webSocket, tx))
      self ! pendingAction

    case OnFailedOpen(tx, message, _) =>
      context.become(crashedState(tx, s"Websocket '$wsName' originally crashed with status $status and message $message and failed to reconnect: $message"))
      self ! pendingAction

    case unexpected =>
      // FIXME we're losing check timeout!
      logger.info(s"Discarding unknown message $unexpected while in reconnecting state")
  }

  def crashedState(tx: AsyncTx, error: String): Receive = {

    case action: WsUserAction =>
      import action._
      val now = nowMillis
      logResponse(session, requestName, KO, now, now, Some(error))
      next ! session.update(tx.updates).markAsFailed.remove(wsName)
      context.stop(self)

    case unexpected =>
      logger.info(s"Discarding unknown message $unexpected while in crashed state")
  }
}
