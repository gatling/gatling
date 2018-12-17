/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.polling

import java.nio.charset.Charset

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.stats.StatsEngine
import io.gatling.http.cache.HttpCaches
import io.gatling.http.engine.response._
import io.gatling.http.engine.tx.{ HttpTx, HttpTxExecutor }
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.HttpRequestDef
import io.gatling.http.response.{ HttpResult, ResponseBuilderFactory }

import akka.actor.Props

case class FetchedResource(
    tx:     HttpTx,
    result: HttpResult
)

object PollerActor {
  def props(
    pollerName:             String,
    period:                 FiniteDuration,
    requestDef:             HttpRequestDef,
    responseBuilderFactory: ResponseBuilderFactory,
    httpTxExecutor:         HttpTxExecutor,
    httpCaches:             HttpCaches,
    httpProtocol:           HttpProtocol,
    statsEngine:            StatsEngine,
    clock:                  Clock,
    charset:                Charset
  ): Props =
    Props(new PollerActor(pollerName, period, requestDef, responseBuilderFactory, httpTxExecutor, httpCaches, httpProtocol, statsEngine, clock, charset))

  private[polling] val PollTimerName = "pollTimer"
}

class PollerActor(
    pollerName:             String,
    period:                 FiniteDuration,
    requestDef:             HttpRequestDef,
    responseBuilderFactory: ResponseBuilderFactory,
    httpTxExecutor:         HttpTxExecutor,
    httpCaches:             HttpCaches,
    httpProtocol:           HttpProtocol,
    statsEngine:            StatsEngine,
    clock:                  Clock,
    charset:                Charset
) extends PollerFSM {

  import PollerActor.PollTimerName

  startWith(Uninitialized, NoData)

  when(Uninitialized) {
    case Event(StartPolling(session), NoData) =>
      resetTimer()
      goto(Polling) using PollingData(session)
  }

  when(Polling) {
    case Event(Poll, PollingData(session)) =>
      val outcome = for {
        requestName <- requestDef.requestName(session).mapError { errorMessage =>
          logger.error(s"'${self.path.name}' failed to execute: $errorMessage")
          errorMessage
        }

        httpRequest <- requestDef.build(requestName, session).mapError { errorMessage =>
          statsEngine.reportUnbuildableRequest(session, pollerName, errorMessage)
          errorMessage
        }
      } yield {
        val nonBlockingTx = HttpTx(session, httpRequest, responseBuilderFactory, next = null, resourceTx = None)
        httpTxExecutor.execute(nonBlockingTx, (tx: HttpTx) => new ResponseProcessor() {
          override def onComplete(result: HttpResult): Unit = {
            self ! FetchedResource(tx, result)
          }
        })
      }

      outcome match {
        case _: Success[_] =>
          stay()
        case _ =>
          resetTimer()
          stay() using PollingData(session.markAsFailed)
      }

    case Event(FetchedResource(tx, result), PollingData(session)) =>
      resetTimer()

      val newSession = new PollerResponseProcessor(
        tx.copy(session = session),
        sessionProcessor = new RootSessionProcessor(
          !tx.silent,
          tx.request.clientRequest,
          tx.request.requestConfig.checks,
          httpCaches,
          httpProtocol,
          clock
        ),
        statsProcessor = httpTxExecutor.statsProcessor(tx),
        charset
      ).onComplete(result)

      stay() using PollingData(newSession)

    case Event(StopPolling(nextActor, session), PollingData(_)) =>
      cancelTimer(PollTimerName)
      nextActor ! session.remove(pollerName)
      stop()
  }

  whenUnhandled {
    case Event(message, state) =>
      logger.debug(s"Can't handle $message in state $state")
      stay()
  }

  initialize()

  private def resetTimer(): Unit =
    setTimer(PollTimerName, Poll, period)
}
