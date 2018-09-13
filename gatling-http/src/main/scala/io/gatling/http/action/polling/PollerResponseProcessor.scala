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

import scala.util.control.NonFatal

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen
import io.gatling.http.HeaderNames
import io.gatling.http.engine.response._
import io.gatling.http.engine.tx.HttpTx
import io.gatling.http.response.{ HttpFailure, HttpResult, Response }
import io.gatling.http.util.HttpHelper
import io.gatling.http.util.HttpHelper.resolveFromUri

import com.softwaremill.quicklens._
import com.typesafe.scalalogging.StrictLogging

class PollerResponseProcessor(
    tx:               HttpTx,
    sessionProcessor: SessionProcessor,
    statsProcessor:   StatsProcessor,
    defaultCharset:   Charset
) extends StrictLogging with NameGen {

  def onComplete(result: HttpResult): (Session, Session => Session) =
    result match {
      case response: Response   => handleResponse(response)
      case failure: HttpFailure => handleFailure(failure)
    }

  private def handleFailure(failure: HttpFailure): (Session, Session => Session) = {
    val sessionWithUpdatedStats = sessionProcessor.updateSessionCrashed(tx.session, failure.startTimestamp, failure.endTimestamp)
    val updates = sessionProcessor.updateSessionCrashed(_: Session, failure.startTimestamp, failure.endTimestamp)
    try {
      statsProcessor.reportStats(tx.fullRequestName, tx.request.clientRequest, sessionWithUpdatedStats, KO, failure, Some(failure.errorMessage))
    } catch {
      case NonFatal(t) =>
        logger.error(s"ResponseProcessor crashed while handling failure $failure on session=${tx.session} request=${tx.request.requestName}: ${tx.request.clientRequest}, forwarding", t)
    }
    (sessionWithUpdatedStats, updates)
  }

  private def handleResponse(response: Response): (Session, Session => Session) = {
    val clientRequest = tx.request.clientRequest
    handleResponse0(response) match {
      case Proceed(newSession, updates, errorMessage) =>
        // different from tx.status because tx could be silent
        val status = if (errorMessage.isDefined) KO else OK
        statsProcessor.reportStats(tx.fullRequestName, clientRequest, newSession, status, response, errorMessage)
        (newSession, updates)

      case Redirect(redirectTx) =>
        statsProcessor.reportStats(tx.fullRequestName, clientRequest, redirectTx.session, OK, response, None)
        logger.error("Polling support doesn't support redirect atm")
        (tx.session.markAsFailed, Session.Identity)

      case Crash(errorMessage) =>
        val newSession = sessionProcessor.updateSessionCrashed(tx.session, response.startTimestamp, response.endTimestamp)
        val updates = sessionProcessor.updateSessionCrashed(_: Session, response.startTimestamp, response.endTimestamp)
        statsProcessor.reportStats(tx.fullRequestName, clientRequest, newSession, KO, response, Some(errorMessage))
        (newSession, updates)
    }
  }

  private def handleResponse0(response: Response): ProcessorResult =
    try {
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
        val (newSession, updates, errorMessage) = sessionProcessor.updatedSession(tx.session, response, computeUpdates = true)
        Proceed(newSession, updates, errorMessage)
      }
    } catch {
      case NonFatal(t) =>
        logger.error(s"ResponseProcessor crashed while handling response ${response.status} on session=${tx.session} request=${tx.request.requestName}: ${tx.request.clientRequest}, forwarding", t)
        Crash(t.detailedMessage)
    }
}
