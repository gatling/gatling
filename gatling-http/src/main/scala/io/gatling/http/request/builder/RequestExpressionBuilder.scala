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

package io.gatling.http.request.builder

import java.nio.charset.Charset

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.cache.{ BaseUrlSupport, HttpCaches, LocalAddressSupport }
import io.gatling.http.client.{ Request, SignatureCalculator, RequestBuilder => ClientRequestBuilder }
import io.gatling.http.client.uri.{ Uri, UriEncoder }
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.referer.RefererHandling
import io.gatling.http.util.HttpHelper

import com.typesafe.scalalogging.LazyLogging
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.util.AsciiString

object RequestExpressionBuilder {
  val BuildRequestErrorMapper: String => String = "Failed to build request: " + _

  type RequestBuilderConfigureRaw = Session => ClientRequestBuilder => ClientRequestBuilder
  type RequestBuilderConfigure = Session => ClientRequestBuilder => Validation[ClientRequestBuilder]

  val ConfigureIdentityRaw: RequestBuilderConfigureRaw = _ => identity
  val ConfigureIdentity: RequestBuilderConfigure = _ => _.success
}

abstract class RequestExpressionBuilder(
    commonAttributes: CommonAttributes,
    httpCaches: HttpCaches,
    httpProtocol: HttpProtocol,
    configuration: GatlingConfiguration
) extends LazyLogging {

  import RequestExpressionBuilder._

  protected val charset: Charset = configuration.core.charset
  protected val headers: Map[CharSequence, Expression[String]] =
    if (commonAttributes.ignoreProtocolHeaders) {
      commonAttributes.headers
    } else {
      httpProtocol.requestPart.headers ++ commonAttributes.headers
    }
  private val refererHeaderIsUndefined: Boolean = !headers.keys.exists(AsciiString.contentEqualsIgnoreCase(_, HttpHeaderNames.REFERER))
  protected val contentTypeHeaderIsUndefined: Boolean = !headers.keys.exists(AsciiString.contentEqualsIgnoreCase(_, HttpHeaderNames.CONTENT_TYPE))
  private val fixUrlEncoding: Boolean = !commonAttributes.disableUrlEncoding.getOrElse(httpProtocol.requestPart.disableUrlEncoding)
  private val signatureCalculatorExpression: Option[Expression[SignatureCalculator]] =
    commonAttributes.signatureCalculator.orElse(httpProtocol.requestPart.signatureCalculator)

  private val baseUrl: Session => Option[String] = protocolBaseUrl

  protected def protocolBaseUrl: Session => Option[String] =
    BaseUrlSupport.httpBaseUrl(httpProtocol)

  protected def protocolBaseUrls: List[String] =
    httpProtocol.baseUrls

  protected def isAbsoluteUrl(url: String): Boolean =
    HttpHelper.isAbsoluteHttpUrl(url)

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
    if (isAbsoluteUrl(url))
      Uri.create(url).success
    else
      resolveRelativeAgainstBaseUrl(url, baseUrl(session))

  private val buildURI: Expression[Uri] = {
    val queryParams = commonAttributes.queryParams

    commonAttributes.urlOrURI match {
      case Left(StaticValueExpression(staticUrl)) if protocolBaseUrls.size <= 1 && queryParams.isEmpty =>
        if (isAbsoluteUrl(staticUrl)) {
          Uri.create(staticUrl).expressionSuccess
        } else {
          val uriV = resolveRelativeAgainstBaseUrl(staticUrl, protocolBaseUrls.headOption)
          _ => uriV
        }

      case Left(url) =>
        // url is not static, or multiple baseUrl, or queryParams
        session =>
          for {
            resolvedUrl <- url(session)
            absoluteUri <- makeAbsolute(session, resolvedUrl)
            resolvedQueryParams <- queryParams.resolveParamJList(session)
          } yield UriEncoder.uriEncoder(fixUrlEncoding).encode(absoluteUri, resolvedQueryParams)

      case Right(uri) => uri.expressionSuccess
    }
  }

  private val proxy = commonAttributes.proxy.orElse(httpProtocol.proxyPart.proxy)

  private def configureProxy(requestBuilder: ClientRequestBuilder): Unit =
    proxy.foreach { proxy =>
      if (!httpProtocol.proxyPart.proxyExceptions.contains(requestBuilder.getUri.getHost)) {
        requestBuilder.setProxyServer(proxy)
      }
    }

  private def configureCookies(session: Session, requestBuilder: ClientRequestBuilder): Unit = {
    val cookies = CookieSupport.getStoredCookies(session, requestBuilder.getUri)
    if (cookies.nonEmpty) {
      requestBuilder.setCookies(cookies.asJava)
    }
  }

  private val configureVirtualHost: RequestBuilderConfigure =
    commonAttributes.virtualHost.orElse(httpProtocol.enginePart.virtualHost) match {
      case None              => ConfigureIdentity
      case Some(virtualHost) => configureVirtualHost0(virtualHost)
    }

  private def configureVirtualHost0(virtualHost: Expression[String]): RequestBuilderConfigure =
    session => requestBuilder => virtualHost(session).map(requestBuilder.setVirtualHost)

  protected def addDefaultHeaders(session: Session)(requestBuilder: ClientRequestBuilder): ClientRequestBuilder = {
    if (httpProtocol.requestPart.autoReferer && refererHeaderIsUndefined) {
      RefererHandling.getStoredReferer(session).map(requestBuilder.addHeader(HttpHeaderNames.REFERER, _))
    }
    requestBuilder
  }

  private val configureHeaders: RequestBuilderConfigure =
    if (headers.isEmpty)
      session => addDefaultHeaders(session)(_).success
    else {
      val staticHeaders = headers.collect { case (key, StaticValueExpression(value)) => key -> value }

      if (staticHeaders.size == headers.size)
        configureStaticHeaders(staticHeaders)
      else
        configureDynamicHeaders
    }

  private def configureStaticHeaders(staticHeaders: Iterable[(CharSequence, String)]): RequestBuilderConfigure = {
    val addHeaders: ClientRequestBuilder => Validation[ClientRequestBuilder] = requestBuilder => {
      staticHeaders.foreach { case (key, value) => requestBuilder.addHeader(key, value) }
      requestBuilder.success
    }
    session => requestBuilder => addHeaders(requestBuilder).map(addDefaultHeaders(session))
  }

  private def configureDynamicHeaders: RequestBuilderConfigure =
    session =>
      requestBuilder => {
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
      case _           => ConfigureIdentity
    }

  private def configureLocalAddress(session: Session, requestBuilder: ClientRequestBuilder): Unit = {
    if (httpProtocol.enginePart.localIpV4Addresses.nonEmpty) {
      LocalAddressSupport.localIpV4Address(session).foreach(requestBuilder.setLocalIpV4Address)
    }
    if (httpProtocol.enginePart.localIpV6Addresses.nonEmpty) {
      LocalAddressSupport.localIpV6Address(session).foreach(requestBuilder.setLocalIpV6Address)
    }
  }

  private val configureSignatureCalculator: RequestBuilderConfigure =
    signatureCalculatorExpression match {
      case Some(signatureCalculator) =>
        session => requestBuilder => signatureCalculator(session).map(requestBuilder.setSignatureCalculator)
      case _ => ConfigureIdentity
    }

  protected def configureRequestTimeout(requestBuilder: ClientRequestBuilder): Unit

  protected def configureRequestBuilderForProtocol: RequestBuilderConfigure

  private def configureRequestBuilder(session: Session, requestBuilder: ClientRequestBuilder): Validation[ClientRequestBuilder] = {
    configureProxy(requestBuilder)
    configureRequestTimeout(requestBuilder)
    configureCookies(session, requestBuilder)
    configureLocalAddress(session, requestBuilder)

    configureVirtualHost(session)(requestBuilder)
      .flatMap(configureHeaders(session))
      .flatMap(configureRealm(session))
      .flatMap(configureSignatureCalculator(session))
      .flatMap(configureRequestBuilderForProtocol(session))
  }

  def build: Expression[Request] =
    session =>
      safely(BuildRequestErrorMapper) {
        for {
          uri <- buildURI(session)
          nameResolver <- httpCaches.nameResolver(session) // note: DNS cache is supposed to be set early
          requestBuilder = new ClientRequestBuilder(commonAttributes.method, uri, nameResolver).setDefaultCharset(charset)
          rb <- configureRequestBuilder(session, requestBuilder)
        } yield rb.build
      }
}
