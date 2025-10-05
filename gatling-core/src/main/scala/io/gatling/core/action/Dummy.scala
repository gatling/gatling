/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.core.action

import java.util.concurrent.TimeUnit

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

private[core] final class Dummy(
    override val requestName: Expression[String],
    responseTimeInMillis: Expression[Int],
    success: Expression[Boolean],
    sessionUpdate: Expression[Session],
    override val statsEngine: StatsEngine,
    override val clock: Clock,
    override val next: Action
) extends RequestAction
    with NameGen {

  override val name: String = genName("dummy")

  private def schedule(
      resolvedRequestName: String,
      responseTimeInMillis: Long,
      startTimestamp: Long,
      endTimestamp: Long,
      status: Status,
      newSession: Session,
      errorMessage: Option[String]
  ): Unit = {
    logger.debug(s"Executing dummy request '$resolvedRequestName': status=$status responseTime=${responseTimeInMillis}ms")
    newSession.eventLoop.schedule(
      (() => {
        statsEngine.logResponse(newSession.scenario, newSession.groups, resolvedRequestName, startTimestamp, endTimestamp, status, None, errorMessage)
        next ! newSession
      }): Runnable,
      responseTimeInMillis,
      TimeUnit.MILLISECONDS
    )
  }

  override def sendRequest(session: Session): Validation[Unit] =
    for {
      resolvedRequestName <- requestName(session)
      newSession <- sessionUpdate(session)
      resolvedResponseTimeInMillis <- responseTimeInMillis(newSession)
      resolvedSuccess <- success(newSession)
    } yield {
      val startTimestamp = clock.nowMillis
      val endTimestamp = startTimestamp + resolvedResponseTimeInMillis
      val (sessionWithUpdatedStatus, status, errorMessage) =
        if (resolvedSuccess) {
          (newSession, OK, None)
        } else {
          (newSession.markAsFailed, KO, Some("Dummy error"))
        }

      val sessionWithUpdatedTimings = sessionWithUpdatedStatus.logGroupRequestTimings(startTimestamp, endTimestamp)
      schedule(resolvedRequestName, resolvedResponseTimeInMillis, startTimestamp, endTimestamp, status, sessionWithUpdatedTimings, errorMessage)
    }
}
