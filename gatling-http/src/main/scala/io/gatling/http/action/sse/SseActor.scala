/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.action.sse

import akka.actor.{ Props, ActorRef }
import io.gatling.core.akka.BaseActor
import io.gatling.core.check.CheckResult
import io.gatling.core.result.message.{ ResponseTimings, KO, OK, Status }
import io.gatling.core.result.writer.StatsEngine
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.Success
import io.gatling.http.ahc.SseTx
import io.gatling.http.check.ws.{ WsCheck, ExpectedCount, ExpectedRange, UntilCount }

import scala.collection.mutable

object SseActor {
  def props(sseName: String, statsEngine: StatsEngine) =
    Props(new SseActor(sseName, statsEngine))
}

class SseActor(sseName: String, statsEngine: StatsEngine) extends BaseActor {

  override def receive = initialState

  def failPendingCheck(tx: SseTx, message: String): SseTx = {
    tx.check match {
      case Some(c) =>
        logRequest(tx.session, tx.requestName, KO, tx.start, nowMillis, Some(message))
        tx.copy(updates = Session.MarkAsFailedUpdate :: tx.updates, pendingCheckSuccesses = Nil, check = None)

      case _ => tx
    }
  }

  def setCheck(tx: SseTx, sseStream: SseStream, requestName: String, check: WsCheck, next: ActorRef, session: Session): Unit = {

    logger.debug(s"setCheck blocking=${check.blocking} timeout=${check.timeout}")

    // schedule timeout
    scheduler.scheduleOnce(check.timeout) {
      self ! CheckTimeout(check)
    }

    val newTx = failPendingCheck(tx, "Check didn't succeed by the time a new one was set up")
      .applyUpdates(session)
      .copy(requestName = requestName, start = nowMillis, check = Some(check), pendingCheckSuccesses = Nil, next = next)
    context.become(openState(sseStream, newTx))

    if (!check.blocking)
      next ! newTx.session
  }

  private def logRequest(session: Session, requestName: String, status: Status, started: Long, ended: Long, errorMessage: Option[String] = None): Unit = {
    val timings = ResponseTimings(started, ended, ended, ended)
    statsEngine.logResponse(session, requestName, timings, status, None, errorMessage)
  }

  val initialState: Receive = {

    case OnOpen(tx, sseStream, time) =>
      import tx._
      logger.debug(s"sse stream '$sseName' open")
      val newSession = session.set(sseName, self)
      val newTx = tx.copy(session = newSession)

      check match {
        case None =>
          logRequest(session, requestName, OK, start, time)
          context.become(openState(sseStream, newTx))
          next ! newSession

        case Some(c) =>
          // hack, reset check so that there's no pending one
          setCheck(newTx.copy(check = None), sseStream, requestName, c, next, newSession)
      }

    case OnFailedOpen(tx, message, end) =>
      import tx._
      logger.debug(s"sse '$sseName' failed to open:$message")
      logRequest(session, requestName, KO, start, end, Some(message))

      next ! session.markAsFailed
      context.stop(self)
  }

  def openState(sseStream: SseStream, tx: SseTx): Receive = {

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
              context.become(openState(sseStream, newTx))

            } else {
              // add to pending updates
              val newTx = tx.copy(updates = newUpdates, check = None, pendingCheckSuccesses = Nil)
              context.become(openState(sseStream, newTx))
            }

          case _ =>
        }
      }

      def reconciliate(next: ActorRef, session: Session): Unit = {
        val newTx = tx.applyUpdates(session)
        context.become(openState(sseStream, newTx))
        next ! newTx.session
      }

    {
      case SetCheck(requestName, check, next, session) =>
        logger.debug(s"Setting check on sse '$sseName'")
        setCheck(tx, sseStream, requestName, check, next, session)

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
              case ExpectedCount(count) if count == tx.pendingCheckSuccesses.size        => succeedPendingCheck(tx.pendingCheckSuccesses)
              case ExpectedRange(range) if range.contains(tx.pendingCheckSuccesses.size) => succeedPendingCheck(tx.pendingCheckSuccesses)
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
                case UntilCount(count) if count == results.length => succeedPendingCheck(results)

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
        reconciliate(next, session)

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
        logRequest(session, requestName, KO, start, end, Some(message))

        next ! session.remove(sseName)

        context.stop(self)

      case unexpected =>
        logger.error(s"Discarding unknown message $unexpected while in open state")
    }
  }

  def closingState(tx: SseTx): Receive = {
    case OnClose =>
      import tx._
      logRequest(session, requestName, OK, start, nowMillis)
      next ! session.remove(sseName)
      context.stop(self)

    case unexpected =>
      logger.error(s"Discarding unknown message $unexpected while in closing state")
  }
}
