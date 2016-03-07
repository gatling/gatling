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
package io.gatling.http.action.async

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.Maps._
import io.gatling.commons.util.TimeHelper._
import io.gatling.core.action.Action
import io.gatling.core.akka.BaseActor
import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.http.check.async.AsyncCheck

abstract class AsyncProtocolActor(statsEngine: StatsEngine) extends BaseActor {

  protected type NextTxBasedBehaviour = AsyncTx => this.Receive

  protected def failPendingCheck(tx: AsyncTx, message: String): AsyncTx = {
    tx.check match {
      case Some(c) =>
        logResponse(tx.session, tx.requestName, KO, tx.start, nowMillis, Some(message))
        tx.copy(updates = Session.MarkAsFailedUpdate :: tx.updates, pendingCheckSuccesses = Nil, check = None)

      case _ => tx
    }
  }

  protected def setCheck(tx: AsyncTx, requestName: String, check: AsyncCheck,
                         next: Action, session: Session, nextState: NextTxBasedBehaviour): Unit = {
    logger.debug(s"setCheck blocking=${check.blocking} timeout=${check.timeout}")

    // schedule timeout
    scheduler.scheduleOnce(check.timeout) {
      self ! CheckTimeout(check)
    }

    val newTx = failPendingCheck(tx, "Check didn't succeed by the time a new one was set up")
      .applyUpdates(session)
      .copy(requestName = requestName, start = nowMillis, check = Some(check), pendingCheckSuccesses = Nil, next = next)
    context.become(nextState(newTx))

    if (!check.blocking)
      next ! newTx.session
  }

  protected def succeedPendingCheck(tx: AsyncTx, results: List[CheckResult], nextState: NextTxBasedBehaviour): Unit = {
    tx.check match {
      case Some(check) =>
        // expected count met, let's stop the check
        logResponse(tx.session, tx.requestName, OK, tx.start, nowMillis, None)

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
              .forceMapValues(_.flatMap(_._2 match {
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
          context.become(nextState(newTx))

        } else {
          // add to pending updates
          val newTx = tx.copy(updates = newUpdates, check = None, pendingCheckSuccesses = Nil)
          context.become(nextState(newTx))
        }

      case _ =>
    }
  }

  protected def reconciliate(tx: AsyncTx, next: Action, session: Session, nextState: NextTxBasedBehaviour): Unit = {
    val newTx = tx.applyUpdates(session)
    context.become(nextState(newTx))
    next ! newTx.session
  }

  protected def logResponse(session: Session, requestName: String, status: Status,
                            startDate: Long, endDate: Long, errorMessage: Option[String] = None): Unit =
    statsEngine.logResponse(session, requestName, ResponseTimings(startDate, endDate), status, None, errorMessage)
}
