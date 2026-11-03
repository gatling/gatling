/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import scala.jdk.CollectionConverters._

import io.gatling.core.session.Session
import io.gatling.http.auth.DigestAuthSupport
import io.gatling.http.cache.Http2PriorKnowledgeSupport
import io.gatling.http.client.{ Request, RequestBuilder }
import io.gatling.http.client.realm.DigestRealm
import io.gatling.http.client.uri.Uri
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.protocol.{ HttpProtocol, Remote }

import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaders, HttpMethod, HttpResponseStatus }
import io.netty.handler.codec.http.HttpMethod._
import io.netty.handler.codec.http.HttpResponseStatus._

object FollowUpProcessor {

  /**
   * Builds a follow-up request that keeps the original request's settings: only the request line, the headers and the body differ, and the cookies and the
   * HTTP/2 prior knowledge are the ones for the follow-up's remote.
   */
  private def followUpRequestBuilder(
      name: String,
      method: HttpMethod,
      uri: Uri,
      headers: HttpHeaders,
      originalRequest: Request,
      session: Session,
      defaultCharset: Charset,
      useProxy: Boolean
  ): RequestBuilder = {
    val requestBuilder =
      new RequestBuilder(name, method, uri, originalRequest.getNameResolver)
        .setHeaders(headers)
        .setAutoOrigin(originalRequest.isAutoOrigin)
        .setLocalAddresses(originalRequest.getLocalAddresses)
        .setRealm(originalRequest.getRealm)
        .setRequestTimeout(originalRequest.getRequestTimeout)
        .setSignatureCalculator(originalRequest.getSignatureCalculator)
        .setHttp2Enabled(originalRequest.isHttp2Enabled)
        .setHttp2PriorKnowledge(Http2PriorKnowledgeSupport.findHttp2PriorKnowledge(session, Remote(uri)).orNull)
        .setHttp2FlowControlWindows(originalRequest.getHttp2FlowControlWindows)
        .setDefaultCharset(defaultCharset)

    if (useProxy) {
      requestBuilder.setProxyServer(originalRequest.getProxyServer)
      requestBuilder.setProxyProtocolSourceIpV4Address(originalRequest.getProxyProtocolSourceIpV4Address)
      requestBuilder.setProxyProtocolSourceIpV6Address(originalRequest.getProxyProtocolSourceIpV6Address)
    }

    val cookies = CookieSupport.getStoredCookies(session, uri)
    if (cookies.nonEmpty) {
      requestBuilder.setCookies(cookies.asJava)
    }

    requestBuilder
  }

  /**
   * A copy of the original request's headers, so the original request isn't altered, without the ones the follow-up recomputes.
   */
  private def followUpHeaders(originalRequest: Request): HttpHeaders = {
    val headers = originalRequest.getHeaders
      .copy()
      .remove(HttpHeaderNames.CONTENT_LENGTH)
      .remove(HttpHeaderNames.COOKIE)

    if (originalRequest.getRealm != null) {
      // remove Authorization header if there's a realm as it will be recomputed
      headers.remove(HttpHeaderNames.AUTHORIZATION)
    }

    headers
  }

  def redirectRequest(
      requestName: String,
      originalRequest: Request,
      session: Session,
      responseStatus: HttpResponseStatus,
      httpProtocol: HttpProtocol,
      redirectUri: Uri,
      defaultCharset: Charset,
      redirectCount: Int
  ): Request = {
    val originalMethod = originalRequest.getMethod

    val switchToGet =
      originalMethod != GET && originalMethod != HEAD && originalMethod != OPTIONS && (responseStatus == HttpResponseStatus.MOVED_PERMANENTLY || responseStatus == SEE_OTHER || (responseStatus == FOUND && !httpProtocol.responsePart.strict302Handling))
    val keepBody =
      responseStatus == TEMPORARY_REDIRECT || responseStatus == PERMANENT_REDIRECT || (responseStatus == FOUND && httpProtocol.responsePart.strict302Handling)

    val headers = followUpHeaders(originalRequest)
      // the remote might be different
      .remove(HttpHeaderNames.HOST)
      .remove(HttpHeaderNames.ORIGIN)

    if (!keepBody) {
      headers.remove(HttpHeaderNames.CONTENT_TYPE)
    }

    val requestBuilder =
      followUpRequestBuilder(
        httpProtocol.responsePart.redirectNamingStrategy(redirectUri, requestName, redirectCount),
        if (switchToGet) GET else originalMethod,
        redirectUri,
        headers,
        originalRequest,
        session,
        defaultCharset,
        useProxy = !httpProtocol.proxyPart.proxyExceptions.contains(redirectUri.getHost)
      )

    if (keepBody) {
      Option(originalRequest.getBody).foreach(body => requestBuilder.setBodyBuilder(body.newBuilder))
    }

    requestBuilder.build
  }

  def digestChallengeRequest(
      requestName: String,
      originalRequest: Request,
      session: Session,
      digestRealm: DigestRealm,
      defaultCharset: Charset
  ): Request = {
    val requestBuilder =
      followUpRequestBuilder(
        requestName,
        originalRequest.getMethod,
        originalRequest.getUri,
        followUpHeaders(originalRequest)
          // always recomputed from the challenge
          .remove(HttpHeaderNames.AUTHORIZATION),
        originalRequest,
        session,
        defaultCharset,
        useProxy = true
      )
        .setRealm(DigestAuthSupport.realmWithAuthorizationGen(session, digestRealm))

    Option(originalRequest.getBody).foreach(body => requestBuilder.setBodyBuilder(body.newBuilder))

    requestBuilder.build
  }

  val ExpectationFailedSuffix = " Retry Without Expect"

  /**
   * RFC 9110 section 10.1.1: a 417 in response to a 100-continue expectation means the chain doesn't support expectations, so the request should be repeated
   * without it.
   */
  def expectationFailedRequest(originalRequest: Request, session: Session, defaultCharset: Charset): Request = {
    val requestBuilder =
      followUpRequestBuilder(
        originalRequest.getName + ExpectationFailedSuffix,
        originalRequest.getMethod,
        originalRequest.getUri,
        followUpHeaders(originalRequest).remove(HttpHeaderNames.EXPECT),
        originalRequest,
        session,
        defaultCharset,
        useProxy = true
      )

    Option(originalRequest.getBody).foreach(body => requestBuilder.setBodyBuilder(body.newBuilder))

    requestBuilder.build
  }
}
