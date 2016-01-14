/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.http.request.builder

import java.net.InetAddress

import scala.util.control.NonFatal

import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.HeaderNames
import io.gatling.http.ahc.{ AhcRequestBuilder, AhcChannelPoolPartitioning }
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.referer.RefererHandling

import com.typesafe.scalalogging.LazyLogging
import org.asynchttpclient.{ Realm, Request }
import org.asynchttpclient.uri.Uri

object RequestExpressionBuilder {
  val BuildRequestErrorMapper = "Failed to build request: " + _

  type RequestBuilderConfigureRaw = Session => AhcRequestBuilder => AhcRequestBuilder
  type RequestBuilderConfigure = Session => AhcRequestBuilder => Validation[AhcRequestBuilder]

  val ConfigureIdentityRaw: RequestBuilderConfigureRaw = session => requestBuilder => requestBuilder
  val ConfigureIdentity: RequestBuilderConfigure = session => requestBuilder => requestBuilder.success
}

abstract class RequestExpressionBuilder(commonAttributes: CommonAttributes, httpComponents: HttpComponents)
    extends LazyLogging {

  import RequestExpressionBuilder._
  val protocol = httpComponents.httpProtocol
  val httpCaches = httpComponents.httpCaches
  protected val charset = httpComponents.httpEngine.configuration.core.charset

  def makeAbsolute(url: String): Validation[Uri] =
    protocol.makeAbsoluteHttpUri(url)

  def buildURI(session: Session): Validation[Uri] =
    commonAttributes.urlOrURI match {
      case Left(url) =>
        try {
          url(session).flatMap(makeAbsolute)
        } catch {
          // don't use safe in order to save lambda instances
          case NonFatal(e) => s"url $url can't be parsed into a URI: ${e.getMessage}".failure
        }
      case Right(uri) => uri.success
    }

  // note: DNS cache is supposed to be set early
  private val configureNameResolver: RequestBuilderConfigureRaw =
    session => httpCaches.nameResolver(session) match {
      case None => identity // shouldn't happen
      case Some(nameResolver) =>
        // [fl]
        //
        //
        //
        //
        //
        //
        //
        // [fl]
        _.setNameResolver(nameResolver)
      }

  // FIXME resolve proxy presence once
  private def configureProxy(requestBuilder: AhcRequestBuilder, uri: Uri): Validation[AhcRequestBuilder] = {
    val proxy = commonAttributes.proxy.orElse(protocol.proxyPart.proxy)
    if (proxy.isDefined && !protocol.proxyPart.proxyExceptions.contains(uri.getHost)) {
      proxy.foreach(requestBuilder.setProxyServer)
    }
    requestBuilder.success
  }

  private def configureCookies(session: Session, uri: Uri)(requestBuilder: AhcRequestBuilder): AhcRequestBuilder = {
    CookieSupport.getStoredCookies(session, uri).foreach(requestBuilder.addCookie)
    requestBuilder
  }

  private val configureQuery: RequestBuilderConfigure =
    commonAttributes.queryParams match {
      case Nil         => ConfigureIdentity
      case queryParams => configureQuery0(queryParams)
    }

  private def configureQuery0(queryParams: List[HttpParam]): RequestBuilderConfigure =
    session => requestBuilder => queryParams.resolveParamJList(session).map(requestBuilder.addQueryParams)

  private val configureVirtualHost: RequestBuilderConfigure =
    commonAttributes.virtualHost.orElse(protocol.enginePart.virtualHost) match {
      case None              => ConfigureIdentity
      case Some(virtualHost) => configureVirtualHost0(virtualHost)
    }

  private def configureVirtualHost0(virtualHost: Expression[String]): RequestBuilderConfigure =
    session => requestBuilder => virtualHost(session).map(requestBuilder.setVirtualHost)

  protected def addDefaultHeaders(session: Session, headers: Map[String, Expression[String]])(requestBuilder: AhcRequestBuilder): AhcRequestBuilder = {
    if (!headers.contains(HeaderNames.Referer)) {
      RefererHandling.getStoredReferer(session).map(requestBuilder.addHeader(HeaderNames.Referer, _))
    }
    requestBuilder
  }

  private val configureHeaders: RequestBuilderConfigure = {

    val headers = protocol.requestPart.headers ++ commonAttributes.headers

    if (headers.nonEmpty)
      configureHeaders0(headers)
    else
      ConfigureIdentity
  }

  private def configureHeaders0(headers: Map[String, Expression[String]]): RequestBuilderConfigure =
    session => requestBuilder => {
      val requestBuilderWithHeaders = headers.foldLeft(requestBuilder.success) { (requestBuilder, header) =>
        val (key, value) = header
        for {
          requestBuilder <- requestBuilder
          value <- value(session)
        } yield requestBuilder.addHeader(key, value)
      }

      requestBuilderWithHeaders.map(addDefaultHeaders(session, headers))
    }

  private val configureRealm: RequestBuilderConfigure =
    commonAttributes.realm.orElse(protocol.requestPart.realm) match {
      case Some(realm) => configureRealm0(realm)
      case None        => ConfigureIdentity
    }

  private def configureRealm0(realm: Expression[Realm]): RequestBuilderConfigure =
    session => requestBuilder => realm(session).map(requestBuilder.setRealm)

  val configureLocalAddress: RequestBuilderConfigure =
    commonAttributes.address.orElse(protocol.enginePart.localAddress) match {
      case Some(localAddress) => configureLocalAddress0(localAddress)
      case None               => ConfigureIdentity
    }

  private def configureLocalAddress0(localAddress: Expression[InetAddress]): RequestBuilderConfigure =
    session => requestBuilder => localAddress(session).map(requestBuilder.setLocalAddress)

  protected def configureRequestBuilder(session: Session, uri: Uri, requestBuilder: AhcRequestBuilder): Validation[AhcRequestBuilder] =
    configureProxy(requestBuilder.setUri(uri), uri)
      .map(configureCookies(session, uri))
      .map(configureNameResolver(session))
      .flatMap(configureQuery(session))
      .flatMap(configureVirtualHost(session))
      .flatMap(configureHeaders(session))
      .flatMap(configureRealm(session))
      .flatMap(configureLocalAddress(session))

  def build: Expression[Request] = {

    val disableUrlEncoding = commonAttributes.disableUrlEncoding.getOrElse(protocol.requestPart.disableUrlEncoding)

    (session: Session) => {
      val requestBuilder = new AhcRequestBuilder(commonAttributes.method, disableUrlEncoding)

      requestBuilder.setCharset(charset)

      if (!protocol.enginePart.shareConnections)
        requestBuilder.setChannelPoolPartitioning(new AhcChannelPoolPartitioning(session))

      safely(BuildRequestErrorMapper) {
        for {
          uri <- buildURI(session)
          rb <- configureRequestBuilder(session, uri, requestBuilder)
        } yield rb.build
      }
    }
  }
}
