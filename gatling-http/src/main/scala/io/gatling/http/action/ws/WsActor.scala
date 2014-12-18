/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.ws

import com.ning.http.client.ws.WebSocket

import scala.collection.mutable
import akka.actor.ActorRef
import io.gatling.core.akka.BaseActor
import io.gatling.core.check.CheckResult
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.Success
import io.gatling.http.ahc.{ HttpEngine, WsTx }
import io.gatling.http.check.ws.{ ExpectedRange, UntilCount, ExpectedCount, WsCheck }

class WsActor(wsName: String) extends BaseActor with DataWriterClient {

  def receive = initialState

  def failPendingCheck(tx: WsTx, message: String): WsTx = {
    tx.check match {
      case Some(c) =>
        logRequest(tx.session, tx.requestName, KO, tx.start, nowMillis, Some(message))
        tx.copy(updates = Session.MarkAsFailedUpdate :: tx.updates, pendingCheckSuccesses = Nil, check = None)

      case _ => tx
    }
  }

  private def logRequest(session: Session, requestName: String, status: Status, started: Long, ended: Long, errorMessage: Option[String] = None): Unit = {
    writeRequestData(
      session,
      requestName,
      started,
      ended,
      ended,
      ended,
      status,
      errorMessage)
  }

  def setCheck(tx: WsTx, webSocket: WebSocket, requestName: String, check: WsCheck, next: ActorRef, session: Session): Unit = {

    logger.debug(s"setCheck blocking=${check.blocking} timeout=${check.timeout}")

    // schedule timeout
    scheduler.scheduleOnce(check.timeout) {
      self ! CheckTimeout(check)
    }

    val newTx = failPendingCheck(tx, "Check didn't succeed by the time a new one was set up")
      .applyUpdates(session)
      .copy(requestName = requestName, start = nowMillis, check = Some(check), pendingCheckSuccesses = Nil, next = next)
    context.become(openState(webSocket, newTx))

    if (!check.blocking)
      next ! newTx.session
  }

  val initialState: Receive = {

    case OnOpen(tx, webSocket, end) =>
      import tx._
      logger.debug(s"Websocket '$wsName' open")
      val newSession = session.set(wsName, self)
      val newTx = tx.copy(session = newSession)

      check match {
        case None =>
          logRequest(session, requestName, OK, start, end)
          context.become(openState(webSocket, newTx))
          next ! newSession

        case Some(c) =>
          // hack, reset check so that there's no pending one
          setCheck(newTx.copy(check = None), webSocket, requestName, c, next, newSession)
      }

    case OnFailedOpen(tx, message, end) =>
      import tx._
      logger.debug(s"Websocket '$wsName' failed to open: $message")
      logRequest(session, requestName, KO, start, end, Some(message))
      next ! session.markAsFailed

      context.stop(self)
  }

  def openState(webSocket: WebSocket, tx: WsTx): Receive = {

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
          logRequest(tx.session, tx.requestName, KO, tx.start, time, Some(message))
        }

        context.become(crashedState(tx, message))
      }

      def succeedPendingCheck(results: List[CheckResult]): Unit = {
        tx.check match {
          case Some(check) =>
            // expected count met, let's stop the check
            logRequest(tx.session, tx.requestName, OK, tx.start, nowMillis, None)

            val checkResults = results.filter(_.hasUpdate)

            val newUpdates = checkResults match {
              case Nil =>
                // nothing to save, no update
                tx.updates

              case List(checkResult) =>
                // one single value to save
                checkResult.update.getOrElse(Session.Identity) :: tx.updates

              case _ =>
                // multiple values, let's pile them up
                val mergedCaptures = checkResults
                  .collect { case CheckResult(Some(value), Some(saveAs)) => saveAs -> value }
                  .groupBy(_._1)
                  .mapValues(_.flatMap(_._2 match {
                    case s: Seq[Any] => s
                    case v           => Seq(v)
                  }))

                val newUpdates = (session: Session) => session.setAll(mergedCaptures)
                newUpdates :: tx.updates
            }

            if (check.blocking) {
              // apply updates and release blocked flow
              val newSession = tx.session.update(newUpdates)

              tx.next ! newSession
              val newTx = tx.copy(session = newSession, updates = Nil, check = None, pendingCheckSuccesses = Nil)
              context.become(openState(webSocket, newTx))

            } else {
              // add to pending updates
              val newTx = tx.copy(updates = newUpdates, check = None, pendingCheckSuccesses = Nil)
              context.become(openState(webSocket, newTx))
            }

          case _ =>
        }
      }

      def reconciliate(next: ActorRef, session: Session): Unit = {
        val newTx = tx.applyUpdates(session)
        context.become(openState(webSocket, newTx))
        next ! newTx.session
      }

    {
      case Send(requestName, message, check, next, session) =>
        logger.debug(s"Sending message check on WebSocket '$wsName': $message")

        val now = nowMillis

        check match {
          case Some(c) =>
            // do this immediately instead of self sending a Listen message so that other messages don't get a chance to be handled before
            setCheck(tx, webSocket, requestName + " Check", c, next, session)
          case _ => reconciliate(next, session)
        }

        message match {
          case TextMessage(text)    => webSocket.sendMessage(text)
          case BinaryMessage(bytes) => webSocket.sendMessage(bytes)
        }

        logRequest(session, requestName, OK, now, now)

      case SetCheck(requestName, check, next, session) =>
        logger.debug(s"Setting check on WebSocket '$wsName'")
        setCheck(tx, webSocket, requestName, check, next, session)

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
              case ExpectedCount(count) if count == tx.pendingCheckSuccesses.size        => succeedPendingCheck(tx.pendingCheckSuccesses)
              case ExpectedRange(range) if range.contains(tx.pendingCheckSuccesses.size) => succeedPendingCheck(tx.pendingCheckSuccesses)
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
                case UntilCount(count) if count == results.length => succeedPendingCheck(results)

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
        reconciliate(next, session)

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
        handleClose(status, reason, time)

      case unexpected =>
        logger.info(s"Discarding unknown message $unexpected while in open state")
    }
  }

  def closingState(tx: WsTx): Receive = {
    case m: OnClose =>
      import tx._
      logRequest(session, requestName, OK, start, nowMillis)
      next ! session.remove(wsName)
      context.stop(self)

    case unexpected =>
      logger.info(s"Discarding unknown message $unexpected while in closing state")
  }

  def disconnectedState(status: Int, reason: String, tx: WsTx): Receive = {

    case action: WsUserAction =>
      // reconnect on first client message tentative
      val newTx = tx.copy(reconnectCount = tx.reconnectCount + 1)
      HttpEngine.instance.startWebSocketTransaction(newTx, self)

      context.become(reconnectingState(status, reason, action))

    case unexpected =>
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
      logger.info(s"Discarding unknown message $unexpected while in reconnecting state")
  }

  def crashedState(tx: WsTx, error: String): Receive = {

    case action: WsUserAction =>
      import action._
      val now = nowMillis
      logRequest(session, requestName, KO, now, now, Some(error))
      next ! session.update(tx.updates).markAsFailed.remove(wsName)
      context.stop(self)

    case unexpected =>
      logger.info(s"Discarding unknown message $unexpected while in crashed state")
  }
}
