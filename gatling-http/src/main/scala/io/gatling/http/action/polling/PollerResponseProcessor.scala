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

package io.gatling.http.action.polling

import java.nio.charset.Charset

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen
import io.gatling.http.engine.response._
import io.gatling.http.engine.tx.HttpTx
import io.gatling.http.response.{ HttpFailure, HttpResult, Response }

import com.typesafe.scalalogging.LazyLogging

class PollerResponseProcessor(
    tx: HttpTx,
    sessionProcessor: SessionProcessor,
    statsProcessor: StatsProcessor,
    defaultCharset: Charset
) extends LazyLogging
    with NameGen {

  def onComplete(result: HttpResult): Session =
    result match {
      case response: Response   => proceed(response, ResponseProcessor.processResponse(tx, sessionProcessor, defaultCharset, response))
      case failure: HttpFailure => ResponseProcessor.processFailure(tx, sessionProcessor, statsProcessor, failure)
    }

  private def proceed(response: Response, result: ProcessorResult): Session =
    result match {
      case Proceed(newSession, errorMessage) =>
        // different from tx.status because tx could be silent
        val status = if (errorMessage.isDefined) KO else OK
        statsProcessor.reportStats(tx.fullRequestName, newSession, status, response, errorMessage)
        newSession

      case Redirect(redirectTx) =>
        statsProcessor.reportStats(tx.fullRequestName, redirectTx.session, OK, response, None)
        logger.error("Polling support doesn't support redirect atm")
        tx.session.markAsFailed

      case Crash(errorMessage) =>
        val newSession = sessionProcessor.updateSessionCrashed(tx.session, response.startTimestamp, response.endTimestamp)
        statsProcessor.reportStats(tx.fullRequestName, newSession, KO, response, Some(errorMessage))
        newSession
    }
}
