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
import io.gatling.core.CoreComponents
import io.gatling.core.session._
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

abstract class RequestExpressionBuilder(commonAttributes: CommonAttributes, coreComponents: CoreComponents, httpComponents: HttpComponents)
    extends LazyLogging {

  import RequestExpressionBuilder._
  protected val protocol = httpComponents.httpProtocol
  protected val httpCaches = httpComponents.httpCaches
  protected val configuration = coreComponents.configuration
  protected val charset = configuration.core.charset
  protected val headers = protocol.requestPart.headers ++ commonAttributes.headers
  private val refererHeaderIsUndefined = !headers.contains(HeaderNames.Referer)
  protected val contentTypeHeaderIsUndefined = !headers.contains(HeaderNames.ContentType)
  private val disableUrlEncoding = commonAttributes.disableUrlEncoding.getOrElse(protocol.requestPart.disableUrlEncoding)
  private val signatureCalculatorExpression = commonAttributes.signatureCalculator.orElse(protocol.requestPart.signatureCalculator)

  protected def makeAbsolute(url: String): Validation[Uri] =
    protocol.makeAbsoluteHttpUri(url)

  private val buildURI: Expression[Uri] =
    commonAttributes.urlOrURI match {
      case Left(StaticStringExpression(staticUrl)) if protocol.baseUrls.size <= 1 =>
        val uri = makeAbsolute(staticUrl)
        session => uri

      case Left(url) =>
        session =>
          try {
            url(session).flatMap(makeAbsolute)
          } catch {
            // don't use safe in order to save lambda instances
            case NonFatal(e) => s"url $url can't be parsed into a URI: ${e.getMessage}".failure
          }
      case Right(uri) => uri.expressionSuccess
    }

  // note: DNS cache is supposed to be set early
  private def configureNameResolver(session: Session, requestBuilder: AhcRequestBuilder): Unit =
    configuration.resolve(
      // [fl]
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      // [fl]
      httpCaches.nameResolver(session).foreach(requestBuilder.setNameResolver)
    )

  private def configureChannelPoolPartitioning(session: Session, requestBuilder: AhcRequestBuilder): Unit =
    if (!protocol.enginePart.shareConnections)
      requestBuilder.setChannelPoolPartitioning(new AhcChannelPoolPartitioning(session))

  private val proxy = commonAttributes.proxy.orElse(protocol.proxyPart.proxy)

  private def configureProxy(requestBuilder: AhcRequestBuilder): Unit =
    proxy.foreach { proxy =>
      if (!protocol.proxyPart.proxyExceptions.contains(requestBuilder.getUri.getHost)) {
        requestBuilder.setProxyServer(proxy)
      }
    }

  private def configureCookies(session: Session, requestBuilder: AhcRequestBuilder): Unit =
    CookieSupport.getStoredCookies(session, requestBuilder.getUri).foreach(requestBuilder.addCookie)

  private val configureQueryParams: RequestBuilderConfigure =
    commonAttributes.queryParams match {
      case Nil         => ConfigureIdentity
      case queryParams => configureQueryParams0(queryParams)
    }

  private def configureQueryParams0(queryParams: List[HttpParam]): RequestBuilderConfigure =
    session => requestBuilder => queryParams.resolveParamJList(session).map(requestBuilder.addQueryParams)

  private val configureVirtualHost: RequestBuilderConfigure =
    commonAttributes.virtualHost.orElse(protocol.enginePart.virtualHost) match {
      case None              => ConfigureIdentity
      case Some(virtualHost) => configureVirtualHost0(virtualHost)
    }

  private def configureVirtualHost0(virtualHost: Expression[String]): RequestBuilderConfigure =
    session => requestBuilder => virtualHost(session).map(requestBuilder.setVirtualHost)

  protected def addDefaultHeaders(session: Session)(requestBuilder: AhcRequestBuilder): AhcRequestBuilder = {
    if (protocol.requestPart.autoReferer && refererHeaderIsUndefined) {
      RefererHandling.getStoredReferer(session).map(requestBuilder.addHeader(HeaderNames.Referer, _))
    }
    requestBuilder
  }

  private val configureHeaders: RequestBuilderConfigure =
    if (headers.isEmpty)
      session => addDefaultHeaders(session)(_).success
    else {
      val staticHeaders = headers.collect { case (key, StaticStringExpression(value)) => key -> value }

      if (staticHeaders.size == headers.size)
        configureStaticHeaders(staticHeaders)
      else
        configureDynamicHeaders
    }

  private def configureStaticHeaders(staticHeaders: Iterable[(String, String)]): RequestBuilderConfigure = {
    val addHeaders: AhcRequestBuilder => Validation[AhcRequestBuilder] = requestBuilder => {
      staticHeaders.foreach { case (key, value) => requestBuilder.addHeader(key, value) }
      requestBuilder.success
    }
    session => requestBuilder => addHeaders(requestBuilder).map(addDefaultHeaders(session))
  }

  private def configureDynamicHeaders: RequestBuilderConfigure =
    session => requestBuilder => {
      val requestBuilderWithHeaders = headers.foldLeft(requestBuilder.success) { (requestBuilder, header) =>
        val (key, value) = header
        for {
          requestBuilder <- requestBuilder
          value <- value(session)
        } yield requestBuilder.addHeader(key, value)
      }

      requestBuilderWithHeaders.map(addDefaultHeaders(session))
    }

  private val configureRealm: RequestBuilderConfigure =
    commonAttributes.realm.orElse(protocol.requestPart.realm) match {
      case None        => ConfigureIdentity
      case Some(realm) => configureRealm0(realm)
    }

  private def configureRealm0(realm: Expression[Realm]): RequestBuilderConfigure =
    session => requestBuilder => realm(session).map(requestBuilder.setRealm)

  private def configureLocalAddress(session: Session, requestBuilder: AhcRequestBuilder): Unit =
    if (protocol.enginePart.localAddresses.nonEmpty)
      httpCaches.localAddress(session).foreach(requestBuilder.setLocalAddress)

  protected def configureRequestBuilder(session: Session, uri: Uri, requestBuilder: AhcRequestBuilder): Validation[AhcRequestBuilder] = {

    requestBuilder.setUri(uri)
    configureChannelPoolPartitioning(session, requestBuilder)
    configureProxy(requestBuilder)
    configureCookies(session, requestBuilder)
    configureNameResolver(session, requestBuilder)
    configureLocalAddress(session, requestBuilder)

    configureQueryParams(session)(requestBuilder)
      .flatMap(configureVirtualHost(session))
      .flatMap(configureHeaders(session))
      .flatMap(configureRealm(session))
  }

  private def applySignatureCalculator(session: Session, requestBuilder: AhcRequestBuilder): Validation[Request] = {

    val request = requestBuilder.build

    signatureCalculatorExpression match {
      case None => request.success
      case Some(signatureCalculator) => signatureCalculator(session).map { sc =>
        sc.calculateAndAddSignature(request, requestBuilder)
        requestBuilder.build
      }
    }
  }

  def build: Expression[Request] =
    (session: Session) => {
      val requestBuilder = new AhcRequestBuilder(commonAttributes.method, disableUrlEncoding)
      requestBuilder.setCharset(charset)

      safely(BuildRequestErrorMapper) {
        for {
          uri <- buildURI(session)
          rb <- configureRequestBuilder(session, uri, requestBuilder)
          request <- applySignatureCalculator(session, rb)
        } yield request
      }
    }
}
