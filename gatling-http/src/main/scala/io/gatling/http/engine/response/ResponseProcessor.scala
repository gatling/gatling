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

package io.gatling.http.engine.response

import java.nio.charset.Charset

import scala.util.control.NonFatal

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen
import io.gatling.http.engine.tx.HttpTx
import io.gatling.http.response.{ HttpFailure, HttpResult, Response }
import io.gatling.http.util.HttpHelper
import io.gatling.http.util.HttpHelper.resolveFromUri

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.HttpHeaderNames

sealed trait ProcessorResult
final case class Proceed(newSession: Session, error: Option[String]) extends ProcessorResult
final case class Redirect(redirectTx: HttpTx) extends ProcessorResult
final case class Crash(error: String) extends ProcessorResult

object ResponseProcessor extends StrictLogging {
  def processResponse(tx: HttpTx, sessionProcessor: SessionProcessor, defaultCharset: Charset, response: Response): ProcessorResult =
    try {
      if (HttpHelper.isRedirect(response.status) && tx.request.requestConfig.followRedirect) {
        if (tx.redirectCount >= tx.request.requestConfig.httpProtocol.responsePart.maxRedirects) {
          Crash(s"Too many redirects, max is ${tx.request.requestConfig.httpProtocol.responsePart.maxRedirects}")

        } else {
          val location = response.headers.get(HttpHeaderNames.LOCATION)
          if (location == null) {
            Crash("Redirect status, yet no Location header")
          } else {
            val redirectUri = resolveFromUri(tx.request.clientRequest.getUri, location)
            val newSession = sessionProcessor.updatedRedirectSession(tx.currentSession, response, redirectUri)
            val redirectRequest =
              RedirectProcessor.redirectRequest(
                tx.request.clientRequest,
                newSession,
                response.status,
                tx.request.requestConfig.httpProtocol,
                redirectUri,
                defaultCharset
              )
            Redirect(
              tx.copy(
                session = newSession,
                request = tx.request.copy(clientRequest = redirectRequest),
                redirectCount = tx.redirectCount + 1
              )
            )
          }
        }
      } else {
        val (newSession, errorMessage) = sessionProcessor.updatedSession(tx.currentSession, response)
        Proceed(newSession, errorMessage)
      }
    } catch {
      case NonFatal(t) =>
        logger.error(
          s"ResponseProcessor crashed while handling response ${response.status} on session=${tx.currentSession} request=${tx.request.requestName}: ${tx.request.clientRequest}, forwarding",
          t
        )
        Crash(t.detailedMessage)
    }

  def processFailure(tx: HttpTx, sessionProcessor: SessionProcessor, statsProcessor: StatsProcessor, failure: HttpFailure): Session = {
    val sessionWithUpdatedStats = sessionProcessor.updateSessionCrashed(tx.session, failure.startTimestamp, failure.endTimestamp)
    try {
      statsProcessor.reportStats(tx.fullRequestName, sessionWithUpdatedStats, KO, failure, Some(failure.errorMessage))
    } catch {
      case NonFatal(t) =>
        logger.error(
          s"ResponseProcessor crashed while handling failure $failure on session=${tx.session} request=${tx.request.requestName}: ${tx.request.clientRequest}, forwarding",
          t
        )
    }
    sessionWithUpdatedStats
  }
}

trait ResponseProcessor {
  def onComplete(result: HttpResult): Unit
}

class DefaultResponseProcessor(
    tx: HttpTx,
    sessionProcessor: SessionProcessor,
    statsProcessor: StatsProcessor,
    nextExecutor: NextExecutor,
    defaultCharset: Charset
) extends ResponseProcessor
    with NameGen {

  def onComplete(result: HttpResult): Unit =
    result match {
      case rawResponse: Response =>
        applyResponseTransformer(rawResponse) match {
          case Success(response) =>
            val result = ResponseProcessor.processResponse(tx, sessionProcessor, defaultCharset, response)
            proceed(response, result)

          case Failure(errorMessage) =>
            proceed(rawResponse, Crash(errorMessage))
        }

      case failure: HttpFailure =>
        val newSession = ResponseProcessor.processFailure(tx, sessionProcessor, statsProcessor, failure)
        nextExecutor.executeNextOnCrash(newSession)
    }

  private def proceed(response: Response, result: ProcessorResult): Unit =
    result match {
      case Proceed(newSession, errorMessage) =>
        // different from tx.status because tx could be silent
        val status = if (errorMessage.isDefined) KO else OK
        statsProcessor.reportStats(tx.fullRequestName, newSession, status, response, errorMessage)
        nextExecutor.executeNext(newSession, status, response)

      case Redirect(redirectTx) =>
        statsProcessor.reportStats(tx.fullRequestName, redirectTx.currentSession, OK, response, None)
        nextExecutor.executeRedirect(redirectTx)

      case Crash(errorMessage) =>
        val newSession = sessionProcessor.updateSessionCrashed(tx.currentSession, response.startTimestamp, response.endTimestamp)
        statsProcessor.reportStats(tx.fullRequestName, newSession, KO, response, Some(errorMessage))
        nextExecutor.executeNextOnCrash(newSession)
    }

  private def applyResponseTransformer(rawResponse: Response): Validation[Response] =
    tx.request.requestConfig.responseTransformer match {
      case Some(transformer) =>
        safely("Response transformer crashed: " + _) {
          transformer(tx.currentSession, rawResponse)
        }
      case _ => rawResponse.success
    }
}
