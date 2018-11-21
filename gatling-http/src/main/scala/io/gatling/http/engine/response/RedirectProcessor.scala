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

import scala.collection.JavaConverters._

import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.http.HeaderNames
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.client.{ Request, RequestBuilder => AhcRequestBuilder }
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.protocol.HttpProtocol

import io.netty.handler.codec.http.HttpMethod._
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpResponseStatus._

object RedirectProcessor {

  def redirectRequest(originalRequest: Request, session: Session, responseStatus: HttpResponseStatus, httpProtocol: HttpProtocol, redirectUri: Uri, defaultCharset: Charset): Validation[Request] = {

    val originalMethod = originalRequest.getMethod

    val switchToGet = originalMethod != GET && (responseStatus == HttpResponseStatus.MOVED_PERMANENTLY || responseStatus == SEE_OTHER || (responseStatus == FOUND && !httpProtocol.responsePart.strict302Handling))
    val keepBody = responseStatus == TEMPORARY_REDIRECT || responseStatus == PERMANENT_REDIRECT || (responseStatus == FOUND && httpProtocol.responsePart.strict302Handling)

    val newHeaders = originalRequest.getHeaders
      .remove(HeaderNames.Host)
      .remove(HeaderNames.ContentLength)
      .remove(HeaderNames.Cookie)
      .remove(HeaderNames.Authorization)
      .remove(HeaderNames.Origin)
      .set(HeaderNames.Referer, originalRequest.getUri.toString)

    if (!keepBody) {
      newHeaders.remove(HeaderNames.ContentType)
    }

    val requestBuilder = new AhcRequestBuilder(if (switchToGet) GET else originalMethod, redirectUri)
      .setHeaders(newHeaders)
      .setHttp2Enabled(originalRequest.isHttp2Enabled)
      .setLocalAddress(originalRequest.getLocalAddress)
      .setNameResolver(originalRequest.getNameResolver)
      .setRealm(originalRequest.getRealm)
      .setRequestTimeout(originalRequest.getRequestTimeout)
      .setDefaultCharset(defaultCharset)
      .setFixUrlEncoding(false)

    if (originalRequest.getUri.isSameBase(redirectUri)) {
      // we can only assume the virtual host is still valid if the baseUrl is the same
      requestBuilder.setVirtualHost(originalRequest.getVirtualHost)
    }

    if (!httpProtocol.proxyPart.proxyExceptions.contains(redirectUri.getHost)) {
      val originalRequestProxy = if (originalRequest.getUri.getHost == redirectUri.getHost) Option(originalRequest.getProxyServer) else None
      val protocolProxy = httpProtocol.proxyPart.proxy
      originalRequestProxy.orElse(protocolProxy).foreach(requestBuilder.setProxyServer)
    }

    if (keepBody) {
      Option(originalRequest.getBody).foreach(body => requestBuilder.setBodyBuilder(body.newBuilder))
    }

    val cookies = CookieSupport.getStoredCookies(session, redirectUri)
    if (cookies.nonEmpty) {
      requestBuilder.setCookies(cookies.asJava)
    }

    val newClientRequest = requestBuilder.build

    if (newClientRequest.getUri == originalRequest.getUri
      && newClientRequest.getMethod == originalRequest.getMethod
      && newClientRequest.getCookies.asScala.toSet == originalRequest.getCookies.asScala.toSet) {
      // invalid redirect

      "Invalid redirect to the same request".failure

    } else {
      newClientRequest.success
    }
  }
}
