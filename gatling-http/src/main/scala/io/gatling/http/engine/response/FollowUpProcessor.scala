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

package io.gatling.http.engine.response

import java.nio.charset.Charset

import scala.jdk.CollectionConverters._

import io.gatling.core.session.Session
import io.gatling.http.auth.DigestAuthSupport
import io.gatling.http.client.{ Request, RequestBuilder }
import io.gatling.http.client.realm.DigestRealm
import io.gatling.http.client.uri.Uri
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.protocol.HttpProtocol

import io.netty.handler.codec.http.{ HttpHeaderNames, HttpResponseStatus }
import io.netty.handler.codec.http.HttpMethod._
import io.netty.handler.codec.http.HttpResponseStatus._

object FollowUpProcessor {
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

    val newHeaders = originalRequest.getHeaders
      .remove(HttpHeaderNames.HOST)
      .remove(HttpHeaderNames.CONTENT_LENGTH)
      .remove(HttpHeaderNames.COOKIE)
      .remove(HttpHeaderNames.ORIGIN)

    if (originalRequest.getRealm != null) {
      // remove Authorization header if there's a realm as it will be recomputed
      newHeaders.remove(HttpHeaderNames.AUTHORIZATION)
    }

    if (!keepBody) {
      newHeaders.remove(HttpHeaderNames.CONTENT_TYPE)
    }

    val requestBuilder =
      new RequestBuilder(
        httpProtocol.responsePart.redirectNamingStrategy(redirectUri, requestName, redirectCount),
        if (switchToGet) GET else originalMethod,
        redirectUri,
        originalRequest.getNameResolver
      )
        .setHeaders(newHeaders)
        .setHttp2Enabled(originalRequest.isHttp2Enabled)
        .setLocalAddresses(originalRequest.getLocalAddresses)
        .setRealm(originalRequest.getRealm)
        .setRequestTimeout(originalRequest.getRequestTimeout)
        .setDefaultCharset(defaultCharset)

    if (!httpProtocol.proxyPart.proxyExceptions.contains(redirectUri.getHost)) {
      requestBuilder.setProxyServer(originalRequest.getProxyServer)
      requestBuilder.setProxyProtocolSourceIpV4Address(originalRequest.getProxyProtocolSourceIpV4Address)
      requestBuilder.setProxyProtocolSourceIpV6Address(originalRequest.getProxyProtocolSourceIpV6Address)
    }

    if (keepBody) {
      Option(originalRequest.getBody).foreach(body => requestBuilder.setBodyBuilder(body.newBuilder))
    }

    val cookies = CookieSupport.getStoredCookies(session, redirectUri)
    if (cookies.nonEmpty) {
      requestBuilder.setCookies(cookies.asJava)
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
    val newHeaders = originalRequest.getHeaders
      .remove(HttpHeaderNames.HOST)
      .remove(HttpHeaderNames.CONTENT_LENGTH)
      .remove(HttpHeaderNames.COOKIE)
      .remove(HttpHeaderNames.ORIGIN)
      // remove Authorization header if there's a realm as it will be recomputed
      .remove(HttpHeaderNames.AUTHORIZATION)

    val requestBuilder =
      new RequestBuilder(
        requestName,
        originalRequest.getMethod,
        originalRequest.getUri,
        originalRequest.getNameResolver
      )
        .setHeaders(newHeaders)
        .setHttp2Enabled(originalRequest.isHttp2Enabled)
        .setLocalAddresses(originalRequest.getLocalAddresses)
        .setRealm(originalRequest.getRealm)
        .setRequestTimeout(originalRequest.getRequestTimeout)
        .setDefaultCharset(defaultCharset)
        .setRealm(DigestAuthSupport.realmWithAuthorizationGen(session, digestRealm))

    Option(originalRequest.getBody).foreach(body => requestBuilder.setBodyBuilder(body.newBuilder))

    val cookies = CookieSupport.getStoredCookies(session, originalRequest.getUri)
    if (cookies.nonEmpty) {
      requestBuilder.setCookies(cookies.asJava)
    }

    requestBuilder.build
  }
}
