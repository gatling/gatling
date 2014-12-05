/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.sse

import akka.actor.ActorRef
import io.gatling.core.akka.BaseActor
import io.gatling.core.check.CheckResult
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success }
import io.gatling.http.ahc.SseTx
import io.gatling.http.check.ws.{ ExpectedCount, ExpectedRange, UntilCount }

import scala.collection.mutable

class SseActor(sseName: String) extends BaseActor with DataWriterClient {

  override def receive = initialState(None)

  def failPendingCheck(tx: SseTx, message: String): SseTx = {
    tx.check match {
      case Some(c) =>
        logRequest(tx.session, tx.requestName, KO, tx.start, nowMillis, Some(message))
        tx.copy(updates = Session.MarkAsFailedUpdate :: tx.updates, pendingCheckSuccesses = Nil, check = None)

      case _ => tx
    }
  }

  def initialState(sseSource: Option[SseSource]): Receive = {
    // FIXME should buffer all messages until OnSseSource is received
    case OnSseSource(sseSource) =>
      logger.debug(s"Initiate state with sseSource #${sseSource.hashCode}")
      context.become(initialState(Some(sseSource)))

    case OnSend(tx) =>
      import tx._
      logger.debug(s"sse request '$requestName' has been sent")

      val newSession = session.set(sseName, self)
      val newTx = tx.copy(session = newSession)

      context.become(openState(newTx, sseSource))
      next ! newSession

    case OnFailedOpen(tx, message, end) =>
      import tx._
      logger.debug(s"sse '${sseName}' failed to open:$message")
      logRequest(session, requestName, KO, start, end, Some(message))

      next ! session.markAsFailed
      context.stop(self)
  }

  def openState(tx: SseTx, sseSource: Option[SseSource]): Receive = {

      def stopForwarderAndSucceedPendingCheck(forwarder: SseForwarder, results: List[CheckResult]): Unit = {
        forwarder.stopForward
        succeedPendingCheck(results)
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

              // todo check the start = nowMillis with an example
              val newTx = tx.copy(session = newSession, updates = Nil, check = None, pendingCheckSuccesses = Nil, start = nowMillis)
              context.become(openState(newTx, sseSource))

              newTx.next ! newSession

            } else {
              // add to pending updates
              // todo check the start = nowMillis with an example
              val newTx = tx.copy(updates = newUpdates, check = None, pendingCheckSuccesses = Nil, start = nowMillis)
              context.become(openState(newTx, sseSource))
            }

          case _ =>
        }
      }

      def reconciliate(next: ActorRef, session: Session): Unit = {
        val newTx = tx.applyUpdates(session)
        context.become(openState(newTx, sseSource))
        next ! newTx.session
      }

    {
      case OnSseSource(sseSource) =>
        logger.debug(s"Associate the sseSource #${sseSource.hashCode} to the user #${tx.session.userId}")
        context.become(openState(tx, Some(sseSource)))

      case OnMessage(message, end, forwarder) =>
        logger.debug(s"Received message '$message' for user #${tx.session.userId}")
        import tx._

        tx.check match {
          case Some(c) =>
            implicit val cache = mutable.Map.empty[Any, Any]

            tx.check.foreach { check =>
              val validation = check.check(message, tx.session)

              validation match {
                case Success(result) =>
                  val results = result :: tx.pendingCheckSuccesses

                  check.expectation match {
                    case UntilCount(count) if count == results.length                          => stopForwarderAndSucceedPendingCheck(forwarder, results)
                    case ExpectedCount(count) if count == results.length                       => stopForwarderAndSucceedPendingCheck(forwarder, results)
                    case ExpectedRange(range) if range.contains(tx.pendingCheckSuccesses.size) => stopForwarderAndSucceedPendingCheck(forwarder, results)
                    case _ =>
                      // let's pile up
                      logRequest(session, requestName, OK, start, end) // todo check with an example
                      val newTx = tx.copy(pendingCheckSuccesses = results, start = end)
                      context.become(openState(newTx, sseSource))
                  }

                case Failure(error) =>
                  val newTx = failPendingCheck(tx, error)
                  context.become(openState(newTx, sseSource))
              }
            }

          case None =>
            logRequest(session, requestName, OK, start, end)
            val newTx = tx.copy(start = end)
            context.become(openState(newTx, sseSource))
        }

      case Reconciliate(requestName, next, session) =>
        logger.debug(s"Reconciliating sse '$sseName'")
        reconciliate(next, session)

      case Close(requestName, next, session) =>
        logger.debug(s"Closing sse '$sseName' for user #${session.userId} with sseSource #${sseSource.hashCode}")
        sseSource.foreach(s => s.close)

        val newTx = failPendingCheck(tx, "Check didn't succeed by the time the sse was asked to closed")
          .applyUpdates(session)
          .copy(requestName = requestName, start = nowMillis, next = next)

        context.become(closingState(newTx))

      case OnThrowable(tx, message, end) => {
        import tx._
        logRequest(session, requestName, KO, start, end, Some(message))

        next ! session.remove(sseName)

        context.stop(self)
      }

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
}
