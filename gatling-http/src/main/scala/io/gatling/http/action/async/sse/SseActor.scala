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
package io.gatling.http.action.async.sse

import scala.collection.mutable

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.commons.validation.Success
import io.gatling.core.stats.StatsEngine
import io.gatling.http.action.async._
import io.gatling.http.check.async._

import akka.actor.Props

object SseActor {
  def props(sseName: String, statsEngine: StatsEngine) =
    Props(new SseActor(sseName, statsEngine))
}

class SseActor(sseName: String, statsEngine: StatsEngine) extends AsyncProtocolActor(statsEngine) {

  private def goToOpenState(sseStream: SseStream): NextTxBasedBehaviour =
    tx => openState(sseStream, tx)

  override def receive = initialState

  val initialState: Receive = {

    case OnOpen(tx, sseStream, time) =>
      import tx._
      logger.debug(s"sse stream '$sseName' open")
      val newSession = session.set(sseName, self)
      val newTx = tx.copy(session = newSession)

      check match {
        case None =>
          logResponse(session, requestName, OK, start, time)
          context.become(openState(sseStream, newTx))
          next ! newSession

        case Some(c) =>
          // hack, reset check so that there's no pending one
          setCheck(newTx.copy(check = None), requestName, c, next, newSession, goToOpenState(sseStream))
      }

    case OnFailedOpen(tx, message, end) =>
      import tx._
      logger.debug(s"sse '$sseName' failed to open:$message")
      logResponse(session, requestName, KO, start, end, Some(message))

      next ! session.markAsFailed
      context.stop(self)
  }

  def openState(sseStream: SseStream, tx: AsyncTx): Receive = {
    case SetCheck(requestName, check, next, session) =>
      logger.debug(s"Setting check on sse '$sseName'")
      setCheck(tx, requestName, check, next, session, goToOpenState(sseStream))

    case CancelCheck(requestName, next, session) =>
      logger.debug(s"Cancelling check on sse '$sseName'")

      val newTx = tx
        .applyUpdates(session)
        .copy(check = None, pendingCheckSuccesses = Nil)

      context.become(openState(sseStream, newTx))
      next ! newTx.session

    case CheckTimeout(check) =>
      logger.debug(s"Check on sse '$sseName' timed out")

      tx.check match {
        case Some(`check`) =>
          check.expectation match {
            case ExpectedCount(count) if count == tx.pendingCheckSuccesses.size =>
              succeedPendingCheck(tx, tx.pendingCheckSuccesses, goToOpenState(sseStream))
            case ExpectedRange(range) if range.contains(tx.pendingCheckSuccesses.size) =>
              succeedPendingCheck(tx, tx.pendingCheckSuccesses, goToOpenState(sseStream))
            case _ =>
              val newTx = failPendingCheck(tx, "Check failed: Timeout")
              context.become(openState(sseStream, newTx))

              if (check.blocking)
                // release blocked session
                newTx.next ! newTx.applyUpdates(newTx.session).session
          }

        case _ =>
        // ignore outdated timeout
      }

    case OnMessage(message, time) =>
      logger.debug(s"Received message '$message' for user #${tx.session.userId}")

      tx.check.foreach { check =>

        implicit val cache = mutable.Map.empty[Any, Any]

        check.check(message, tx.session) match {
          case Success(result) =>
            val results = result :: tx.pendingCheckSuccesses

            check.expectation match {
              case UntilCount(count) if count == results.length =>
                succeedPendingCheck(tx, results, goToOpenState(sseStream))

              case _ =>
                // let's pile up
                val newTx = tx.copy(pendingCheckSuccesses = results)
                context.become(openState(sseStream, newTx))
            }

          case _ =>
        }
      }

    case Reconciliate(requestName, next, session) =>
      logger.debug(s"Reconciliating sse '$sseName'")
      reconciliate(tx, next, session, goToOpenState(sseStream))

    case Close(requestName, next, session) =>
      logger.debug(s"Closing sse '$sseName' for user #${session.userId}")
      sseStream.close()

      val newTx = failPendingCheck(tx, "Check didn't succeed by the time the sse was asked to closed")
        .applyUpdates(session)
        .copy(requestName = requestName, start = nowMillis, next = next)

      context.become(closingState(newTx))

    case OnClose =>
      logger.debug(s"Sse '$sseName' closed by the server")
      val newTx = failPendingCheck(tx, "Check didn't succeed by the time the server closed the sse")

      context.become(closingState(newTx))
      self ! OnClose

    case OnThrowable(ttx, message, end) =>
      import ttx._
      logResponse(session, requestName, KO, start, end, Some(message))

      next ! session.remove(sseName)

      context.stop(self)

    case unexpected =>
      logger.error(s"Discarding unknown message $unexpected while in open state")
  }

  def closingState(tx: AsyncTx): Receive = {
    case OnClose =>
      import tx._
      logResponse(session, requestName, OK, start, nowMillis)
      next ! session.remove(sseName)
      context.stop(self)

    case unexpected =>
      logger.error(s"Discarding unknown message $unexpected while in closing state")
  }
}
