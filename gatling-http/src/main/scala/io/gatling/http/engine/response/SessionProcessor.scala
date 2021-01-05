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

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.core.session.Session
import io.gatling.http.cache.{ Http2PriorKnowledgeSupport, HttpCaches }
import io.gatling.http.check.HttpCheck
import io.gatling.http.client.Request
import io.gatling.http.client.uri.Uri
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.referer.RefererHandling
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper

sealed abstract class SessionProcessor(
    silent: Boolean,
    request: Request,
    checks: List[HttpCheck],
    httpCaches: HttpCaches,
    httpProtocol: HttpProtocol
) {

  def updateSessionCrashed(session: Session, startTimestamp: Long, endTimestamp: Long): Session =
    updateSessionStats(session, startTimestamp, endTimestamp, KO)

  private def updateSessionStats(session: Session, startTimestamp: Long, endTimestamp: Long, status: Status): Session =
    if (silent) {
      session
    } else {
      val sessionWithUpdatedStatus =
        if (status == KO) {
          session.markAsFailed
        } else {
          session
        }

      updateGroupRequestTimings(sessionWithUpdatedStatus, startTimestamp, endTimestamp)
    }

  def updatedSession(session: Session, response: Response): (Session, Option[String]) = {

    def updateSessionAfterChecks(s1: Session, status: Status): Session = {
      val s2 = CookieSupport.storeCookies(s1, request.getUri, response.cookies, response.endTimestamp)
      val s3 = updateReferer(s2, response)
      val s4 = httpCaches.cacheContent(s3, httpProtocol, request, response.headers)
      updateSessionStats(s4, response.startTimestamp, response.endTimestamp, status)
    }

    val (sessionWithCheckSavedValues, checkError) = CheckProcessor.check(session, response, checks)
    val sessionWithHttp2PriorKnowledge =
      if (httpProtocol.enginePart.enableHttp2) {
        Http2PriorKnowledgeSupport.updateSessionHttp2PriorKnowledge(sessionWithCheckSavedValues, response)
      } else {
        sessionWithCheckSavedValues
      }

    val newStatus = if (checkError.isDefined) KO else OK
    val newSession = updateSessionAfterChecks(sessionWithHttp2PriorKnowledge, newStatus)
    (newSession, checkError.map(_.message))
  }

  def updatedRedirectSession(session: Session, response: Response, redirectUri: Uri): Session = {
    val sessionWithCookieStoreUpdate = CookieSupport.storeCookies(session, request.getUri, response.cookies, response.endTimestamp)
    val sessionWithGroupUpdate = updateSessionStats(sessionWithCookieStoreUpdate, response.startTimestamp, response.endTimestamp, OK)
    cacheRedirect(sessionWithGroupUpdate, response, redirectUri)
  }

  private def cacheRedirect(session: Session, response: Response, redirectUri: Uri): Session =
    if (httpProtocol.requestPart.cache && HttpHelper.isPermanentRedirect(response.status)) {
      httpCaches.addRedirect(session, request, redirectUri)
    } else {
      session
    }

  protected def updateReferer(session: Session, response: Response): Session
  protected def updateGroupRequestTimings(session: Session, startTimestamp: Long, endTimestamp: Long): Session
}

final class RootSessionProcessor(
    silent: Boolean,
    request: Request,
    checks: List[HttpCheck],
    httpCaches: HttpCaches,
    httpProtocol: HttpProtocol
) extends SessionProcessor(
      silent,
      request,
      checks,
      httpCaches,
      httpProtocol
    ) {
  override protected def updateReferer(session: Session, response: Response): Session =
    RefererHandling.storeReferer(request, response, httpProtocol)(session)

  override protected def updateGroupRequestTimings(session: Session, startTimestamp: Long, endTimestamp: Long): Session =
    session.logGroupRequestTimings(startTimestamp, endTimestamp)
}

final class ResourceSessionProcessor(
    silent: Boolean,
    request: Request,
    checks: List[HttpCheck],
    httpCaches: HttpCaches,
    httpProtocol: HttpProtocol
) extends SessionProcessor(
      silent,
      request,
      checks,
      httpCaches,
      httpProtocol
    ) {
  override protected def updateReferer(session: Session, response: Response): Session = session

  override protected def updateGroupRequestTimings(session: Session, startTimestamp: Long, endTimestamp: Long): Session = session
}
