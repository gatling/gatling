/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder

import com.ning.http.client.uri.Uri
import com.ning.http.client.{ Request, RequestBuilder => AHCRequestBuilder }
import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation._
import io.gatling.http.HeaderNames
import io.gatling.http.ahc.ChannelPoolPartitioning
import io.gatling.http.config.HttpProtocol
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.referer.RefererHandling
import io.gatling.http.util.HttpHelper

import scala.util.control.NonFatal

object RequestExpressionBuilder {
  val BuildRequestErrorMapper = "Failed to build request: " + _
}

abstract class RequestExpressionBuilder(commonAttributes: CommonAttributes, protocol: HttpProtocol)(implicit configuration: GatlingConfiguration) extends StrictLogging {

  import RequestExpressionBuilder._

  def makeAbsolute(url: String): Validation[String]

  def buildURI(session: Session): Validation[Uri] = {

      def createURI(url: String): Validation[Uri] =
        try
          Uri.create(url).success
        catch {
          // don't use executeSafe in order to safe lambda instances
          case NonFatal(e) => s"url $url can't be parsed into a URI: ${e.getMessage}".failure
        }

    commonAttributes.urlOrURI match {
      case Left(url)  => url(session).flatMap(makeAbsolute).flatMap(createURI)
      case Right(uri) => uri.success
    }
  }

  def configureProxy(uri: Uri)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {
    if (!protocol.proxyPart.proxyExceptions.contains(uri.getHost)) {
      val proxies = commonAttributes.proxies.orElse(protocol.proxyPart.proxies)
      proxies.foreach {
        case (httpProxy, httpsProxy) =>
          val proxy = if (HttpHelper.isSecure(uri)) httpsProxy else httpProxy
          requestBuilder.setProxyServer(proxy)
      }
    }
    requestBuilder.success
  }

  def configureCookies(session: Session, uri: Uri)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {
    CookieSupport.getStoredCookies(session, uri).foreach(requestBuilder.addCookie)
    requestBuilder.success
  }

  def configureQuery(session: Session, uri: Uri)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] =
    commonAttributes.queryParams match {
      case Nil         => requestBuilder.success
      case queryParams => queryParams.resolveParamJList(session).map(requestBuilder.addQueryParams)
    }

  def configureVirtualHost(session: Session)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] =
    commonAttributes.virtualHost.orElse(protocol.enginePart.virtualHost) match {
      case None              => requestBuilder.success
      case Some(virtualHost) => virtualHost(session).map(requestBuilder.setVirtualHost)
    }

  def configureHeaders(session: Session)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {

    val headers = protocol.requestPart.headers ++ commonAttributes.headers

    val requestBuilderWithHeaders = headers.foldLeft(requestBuilder.success) { (requestBuilder, header) =>
      val (key, value) = header
      for {
        requestBuilder <- requestBuilder
        value <- value(session)
      } yield requestBuilder.addHeader(key, value)
    }

    val additionalRefererHeader =
      if (headers.contains(HeaderNames.Referer))
        None
      else
        RefererHandling.getStoredReferer(session)

    additionalRefererHeader match {
      case Some(referer) => requestBuilderWithHeaders.map(_.addHeader(HeaderNames.Referer, referer))
      case _             => requestBuilderWithHeaders
    }
  }

  def configureRealm(session: Session)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] =
    commonAttributes.realm.orElse(protocol.requestPart.realm) match {
      case Some(realm) => realm(session).map(requestBuilder.setRealm)
      case None        => requestBuilder.success
    }

  protected def configureRequestBuilder(session: Session, uri: Uri, requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] =
    configureProxy(uri)(requestBuilder.setUri(uri))
      .flatMap(configureCookies(session, uri))
      .flatMap(configureQuery(session, uri))
      .flatMap(configureVirtualHost(session))
      .flatMap(configureHeaders(session))
      .flatMap(configureRealm(session))

  def build: Expression[Request] = {

    val disableUrlEncoding = commonAttributes.disableUrlEncoding.getOrElse(protocol.requestPart.disableUrlEncoding)

    (session: Session) => {
      val requestBuilder = new AHCRequestBuilder(commonAttributes.method, disableUrlEncoding)

      requestBuilder.setBodyEncoding(configuration.core.encoding)

      if (!protocol.enginePart.shareConnections)
        requestBuilder.setConnectionPoolKeyStrategy(new ChannelPoolPartitioning(session))

      protocol.enginePart.localAddress.foreach(requestBuilder.setLocalInetAddress)

      commonAttributes.address.foreach(requestBuilder.setInetAddress)

      executeSafe(BuildRequestErrorMapper) {
        for {
          uri <- buildURI(session)
          rb <- configureRequestBuilder(session, uri, requestBuilder)
        } yield rb.build
      }
    }
  }
}
