/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.action.polling

import scala.concurrent.duration.FiniteDuration

import akka.actor.Props

import io.gatling.core.session.Session
import io.gatling.core.validation._
import io.gatling.http.ahc.{ HttpTx, HttpEngine }
import io.gatling.core.result.writer.DataWriters
import io.gatling.http.fetch.RegularResourceFetched
import io.gatling.http.request.HttpRequestDef
import io.gatling.http.response.ResponseBuilderFactory

object PollerActor {
  def props(pollerName: String,
            period: FiniteDuration,
            requestDef: HttpRequestDef,
            responseBuilderFactory: ResponseBuilderFactory,
            httpEngine: HttpEngine,
            dataWriters: DataWriters): Props =
    Props(new PollerActor(pollerName, period, requestDef, responseBuilderFactory, httpEngine, dataWriters))

  private[polling] val PollTimerName = "pollTimer"
}

class PollerActor(
  pollerName: String,
  period: FiniteDuration,
  requestDef: HttpRequestDef,
  responseBuilderFactory: ResponseBuilderFactory,
  httpEngine: HttpEngine,
  dataWriters: DataWriters)
    extends PollerFSM {

  import PollerActor.PollTimerName

  startWith(Uninitialized, NoData)

  when(Uninitialized) {
    case Event(StartPolling(session), NoData) =>
      resetTimer()
      goto(Polling) using PollingData(session, Session.Identity)
  }

  when(Polling) {
    case Event(Poll, PollingData(session, update)) =>
      val outcome = for {
        requestName <- requestDef.requestName(session).mapError { errorMessage =>
          logger.error(s"'${self.path.name}' failed to execute: $errorMessage")
          errorMessage
        }

        httpRequest <- requestDef.build(requestName, session).mapError { errorMessage =>
          dataWriters.reportUnbuildableRequest(pollerName, session, errorMessage)
          errorMessage
        }

        nonBlockingTx = HttpTx(session, httpRequest, responseBuilderFactory, self, blocking = false)

      } yield httpEngine.startHttpTransaction(nonBlockingTx)

      outcome match {
        case _: Success[_] =>
          stay()
        case _ =>
          resetTimer()
          stay() using PollingData(session.markAsFailed, update andThen Session.MarkAsFailedUpdate)
      }

    case Event(msg: RegularResourceFetched, PollingData(session, update)) =>
      resetTimer()
      stay() using PollingData(msg.sessionUpdates(session), update andThen msg.sessionUpdates)

    case Event(StopPolling(nextActor, session), PollingData(oldSession, update)) =>
      cancelTimer(PollTimerName)
      nextActor ! update(session).remove(pollerName)
      stop()
  }

  initialize()

  private def resetTimer(): Unit =
    setTimer(PollTimerName, Poll, period)
}
