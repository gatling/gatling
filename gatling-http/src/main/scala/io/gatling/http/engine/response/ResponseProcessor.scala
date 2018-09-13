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

package io.gatling.http.engine.response

import java.nio.charset.Charset

import scala.util.control.NonFatal

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen
import io.gatling.http.HeaderNames
import io.gatling.http.engine.tx.HttpTx
import io.gatling.http.response.{ HttpFailure, HttpResult, Response }
import io.gatling.http.util.HttpHelper
import io.gatling.http.util.HttpHelper.resolveFromUri

import com.softwaremill.quicklens._
import com.typesafe.scalalogging.StrictLogging

sealed trait ProcessorResult
case class Proceed(newSession: Session, updates: Session => Session, error: Option[String]) extends ProcessorResult
case class Redirect(redirectTx: HttpTx) extends ProcessorResult
case class Crash(error: String) extends ProcessorResult

trait ResponseProcessor {
  def onComplete(result: HttpResult): Unit
}

class DefaultResponseProcessor(
    tx:               HttpTx,
    sessionProcessor: SessionProcessor,
    statsProcessor:   StatsProcessor,
    nextExecutor:     NextExecutor,
    defaultCharset:   Charset
) extends ResponseProcessor with StrictLogging with NameGen {

  def onComplete(result: HttpResult): Unit =
    result match {
      case response: Response   => handleResponse(response)
      case failure: HttpFailure => handleFailure(failure)
    }

  private def handleFailure(failure: HttpFailure): Unit = {
    val sessionWithUpdatedStats = sessionProcessor.updateSessionCrashed(tx.session, failure.startTimestamp, failure.endTimestamp)
    try {
      statsProcessor.reportStats(tx.fullRequestName, tx.request.clientRequest, sessionWithUpdatedStats, KO, failure, Some(failure.errorMessage))
    } catch {
      case NonFatal(t) =>
        logger.error(s"ResponseProcessor crashed while handling failure $failure on session=${tx.session} request=${tx.request.requestName}: ${tx.request.clientRequest}, forwarding", t)
    } finally {
      nextExecutor.executeNextOnCrash(sessionWithUpdatedStats, failure.endTimestamp)
    }
  }

  private def handleResponse(response: Response): Unit = {
    val clientRequest = tx.request.clientRequest
    handleResponse0(response) match {
      case Proceed(newSession, _, errorMessage) =>
        // different from tx.status because tx could be silent
        val status = if (errorMessage.isDefined) KO else OK
        statsProcessor.reportStats(tx.fullRequestName, clientRequest, newSession, status, response, errorMessage)
        nextExecutor.executeNext(newSession, status, response)

      case Redirect(redirectTx) =>
        statsProcessor.reportStats(tx.fullRequestName, clientRequest, redirectTx.session, OK, response, None)
        nextExecutor.executeRedirect(redirectTx)

      case Crash(errorMessage) =>
        val newSession = sessionProcessor.updateSessionCrashed(tx.session, response.startTimestamp, response.endTimestamp)
        statsProcessor.reportStats(tx.fullRequestName, clientRequest, newSession, KO, response, Some(errorMessage))
        nextExecutor.executeNextOnCrash(newSession, response.endTimestamp)
    }
  }

  private def handleResponseTransformer(rawResponse: Response): Validation[Response] =
    tx.request.requestConfig.responseTransformer match {
      case Some(transformer) =>
        safely("Response transformer crashed: " + _) {
          transformer(tx.session, rawResponse)
        }
      case _ => rawResponse.success
    }

  private def handleResponse0(rawResponse: Response): ProcessorResult =
    try {
      handleResponseTransformer(rawResponse) match {
        case Failure(errorMessage) =>
          Crash(errorMessage)

        case Success(response) =>
          if (HttpHelper.isRedirect(response.status) && tx.request.requestConfig.followRedirect) {
            if (tx.redirectCount >= tx.request.requestConfig.maxRedirects) {
              Crash("Too many redirects, max is " + tx.request.requestConfig.maxRedirects)

            } else {
              response.header(HeaderNames.Location) match {
                case Some(location) =>
                  val redirectUri = resolveFromUri(tx.request.clientRequest.getUri, location)
                  val newSession = sessionProcessor.updatedRedirectSession(tx.session, response, redirectUri)
                  RedirectProcessor.redirectRequest(tx.request.clientRequest, newSession, response.status, tx.request.requestConfig.httpProtocol, redirectUri, defaultCharset) match {
                    case Success(redirectRequest) =>
                      Redirect(tx
                        .modify(_.session).setTo(newSession)
                        .modify(_.request.clientRequest).setTo(redirectRequest)
                        .modify(_.redirectCount).using(_ + 1))

                    case Failure(message) =>
                      Crash(message)
                  }

                case _ =>
                  Crash("Redirect status, yet no Location header")
              }
            }

          } else {
            val (newSession, updates, errorMessage) = sessionProcessor.updatedSession(tx.session, response, computeUpdates = false)
            Proceed(newSession, updates, errorMessage)
          }
      }

    } catch {
      case NonFatal(t) =>
        logger.error(s"ResponseProcessor crashed while handling response ${rawResponse.status} on session=${tx.session} request=${tx.request.requestName}: ${tx.request.clientRequest}, forwarding", t)
        Crash(t.detailedMessage)
    }
}
