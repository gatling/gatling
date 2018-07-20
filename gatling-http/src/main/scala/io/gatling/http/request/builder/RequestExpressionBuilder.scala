/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import java.nio.charset.Charset

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.HeaderNames
import io.gatling.http.cache.HttpCaches
import io.gatling.http.client.{ Request, SignatureCalculator, RequestBuilder => AhcRequestBuilder }
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.referer.RefererHandling
import io.gatling.http.util.HttpHelper

import com.typesafe.scalalogging.LazyLogging

object RequestExpressionBuilder {
  val BuildRequestErrorMapper: String => String = "Failed to build request: " + _

  type RequestBuilderConfigureRaw = Session => AhcRequestBuilder => AhcRequestBuilder
  type RequestBuilderConfigure = Session => AhcRequestBuilder => Validation[AhcRequestBuilder]

  val ConfigureIdentityRaw: RequestBuilderConfigureRaw = _ => identity
  val ConfigureIdentity: RequestBuilderConfigure = _ => _.success
}

abstract class RequestExpressionBuilder(
    commonAttributes: CommonAttributes,
    httpCaches:       HttpCaches,
    httpProtocol:     HttpProtocol,
    configuration:    GatlingConfiguration
)
  extends LazyLogging {

  import RequestExpressionBuilder._

  protected val charset: Charset = configuration.core.charset
  protected val headers: Map[String, Expression[String]] = httpProtocol.requestPart.headers ++ commonAttributes.headers
  private val refererHeaderIsUndefined: Boolean = !headers.contains(HeaderNames.Referer)
  protected val contentTypeHeaderIsUndefined: Boolean = !headers.contains(HeaderNames.ContentType)
  private val disableUrlEncoding: Boolean = commonAttributes.disableUrlEncoding.getOrElse(httpProtocol.requestPart.disableUrlEncoding)
  private val signatureCalculatorExpression: Option[Expression[SignatureCalculator]] = commonAttributes.signatureCalculator.orElse(httpProtocol.requestPart.signatureCalculator)

  protected def baseUrl: Session => Option[String] =
    if (httpProtocol.baseUrls.size <= 1) {
      val baseUrl = httpProtocol.baseUrls.headOption
      _ => baseUrl
    } else {
      httpCaches.baseUrl
    }

  private def resolveRelativeAgainstBaseUrl(relativeUrl: String, baseUrl: Option[String]): Validation[Uri] =
    baseUrl match {
      case Some(base) =>
        val fullUrl = base + relativeUrl
        try {
          Uri.create(fullUrl).success
        } catch {
          // don't use safe in order to save lambda instances
          case NonFatal(e) => s"url $fullUrl can't be parsed into an Uri: ${e.rootMessage}".failure
        }
      case _ => s"No baseUrl defined but provided url is relative : $relativeUrl".failure
    }

  private def makeAbsolute(session: Session, url: String): Validation[Uri] =
    if (HttpHelper.isAbsoluteHttpUrl(url))
      Uri.create(url).success
    else
      resolveRelativeAgainstBaseUrl(url, baseUrl(session))

  private val buildURI: Expression[Uri] =
    commonAttributes.urlOrURI match {
      case Left(StaticStringExpression(staticUrl)) if httpProtocol.baseUrls.size <= 1 =>
        if (HttpHelper.isAbsoluteHttpUrl(staticUrl)) {
          Uri.create(staticUrl).expressionSuccess
        } else {
          val uriV = resolveRelativeAgainstBaseUrl(staticUrl, httpProtocol.baseUrls.headOption)
          _ => uriV
        }

      case Left(url) =>
        session =>
          for {
            resolvedUrl <- url(session)
            absoluteUri <- makeAbsolute(session, resolvedUrl)
          } yield absoluteUri
      case Right(uri) => uri.expressionSuccess
    }

  // note: DNS cache is supposed to be set early
  private def configureNameResolver(session: Session, requestBuilder: AhcRequestBuilder): Unit =
    httpCaches.nameResolver(session).foreach(requestBuilder.setNameResolver)

  private val proxy = commonAttributes.proxy.orElse(httpProtocol.proxyPart.proxy)

  private def configureProxy(requestBuilder: AhcRequestBuilder): Unit =
    proxy.foreach { proxy =>
      if (!httpProtocol.proxyPart.proxyExceptions.contains(requestBuilder.getUri.getHost)) {
        requestBuilder.setProxyServer(proxy)
      }
    }

  private def configureCookies(session: Session, requestBuilder: AhcRequestBuilder): Unit = {
    val cookies = CookieSupport.getStoredCookies(session, requestBuilder.getUri)
    if (cookies.nonEmpty) {
      requestBuilder.setCookies(cookies.asJava)
    }
  }

  private val configureQueryParams: RequestBuilderConfigure =
    commonAttributes.queryParams match {
      case Nil         => ConfigureIdentity
      case queryParams => configureQueryParams0(queryParams)
    }

  private def configureQueryParams0(queryParams: List[HttpParam]): RequestBuilderConfigure =
    session => requestBuilder => queryParams.resolveParamJList(session).map(requestBuilder.setQueryParams)

  private val configureVirtualHost: RequestBuilderConfigure =
    commonAttributes.virtualHost.orElse(httpProtocol.enginePart.virtualHost) match {
      case None              => ConfigureIdentity
      case Some(virtualHost) => configureVirtualHost0(virtualHost)
    }

  private def configureVirtualHost0(virtualHost: Expression[String]): RequestBuilderConfigure =
    session => requestBuilder => virtualHost(session).map(requestBuilder.setVirtualHost)

  protected def addDefaultHeaders(session: Session)(requestBuilder: AhcRequestBuilder): AhcRequestBuilder = {
    if (httpProtocol.requestPart.autoReferer && refererHeaderIsUndefined) {
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
    commonAttributes.realm.orElse(httpProtocol.requestPart.realm) match {
      case Some(realm) => session => requestBuilder => realm(session).map(requestBuilder.setRealm)
      case _ => ConfigureIdentity
    }

  private def configureLocalAddress(session: Session, requestBuilder: AhcRequestBuilder): Unit =
    if (httpProtocol.enginePart.localAddresses.nonEmpty) {
      httpCaches.localAddress(session).foreach(requestBuilder.setLocalAddress)
    }

  private val configureSignatureCalculator: RequestBuilderConfigure =
    signatureCalculatorExpression match {
      case Some(signatureCalculator) =>
        session => requestBuilder => signatureCalculator(session).map(requestBuilder.setSignatureCalculator)
      case _ => ConfigureIdentity
    }

  protected def configureRequestBuilder(session: Session, requestBuilder: AhcRequestBuilder): Validation[AhcRequestBuilder] = {
    configureProxy(requestBuilder)
    configureCookies(session, requestBuilder)
    configureNameResolver(session, requestBuilder)
    configureLocalAddress(session, requestBuilder)

    configureQueryParams(session)(requestBuilder)
      .flatMap(configureVirtualHost(session))
      .flatMap(configureHeaders(session))
      .flatMap(configureRealm(session))
      .flatMap(configureSignatureCalculator(session))
  }

  def build: Expression[Request] =
    (session: Session) => {
      //      requestBuilder.setCharset(charset)

      safely(BuildRequestErrorMapper) {
        for {
          uri <- buildURI(session)
          requestBuilder = new AhcRequestBuilder(commonAttributes.method, uri)
            .setRequestTimeout(configuration.http.ahc.requestTimeout)
            .setHttp2Enabled(httpProtocol.requestPart.enableHttp2)
          rb <- configureRequestBuilder(session, requestBuilder)
        } yield rb.build(configuration.core.charset, !disableUrlEncoding)
      }
    }
}
