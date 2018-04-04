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

package io.gatling.http.engine

import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration._
import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.core.CoreComponents
import io.gatling.core.session._
import io.gatling.core.util.NameGen
import io.gatling.http.HeaderNames._
import io.gatling.http.HeaderValues._
import io.gatling.http.client.{ HttpClient, HttpListener, RequestBuilder }
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.fetch.ResourceFetcher
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.Http

import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaders, HttpMethod, HttpResponseStatus }

object HttpEngine {
  private val AhcAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.ahc"

  def apply(coreComponents: CoreComponents): HttpEngine =
    new HttpEngine(coreComponents, HttpClientFactory(coreComponents), DnsNameResolverFactory(coreComponents))
}

class HttpEngine(
    val coreComponents:         CoreComponents,
    val httpClientFactory:      HttpClientFactory,
    val dnsNameResolverFactory: DnsNameResolverFactory
)
  extends ResourceFetcher with NameGen with StrictLogging {

  def httpClient: HttpClient = httpClientFactory.client

  private[this] var warmedUp = false

  def warmpUp(httpComponents: HttpComponents): Unit =
    if (!warmedUp) {
      logger.info("Start warm up")
      warmedUp = true

      import httpComponents._

      httpProtocol.warmUpUrl match {
        case Some(url) =>
          val requestBuilder = new RequestBuilder(HttpMethod.GET, Uri.create(url))
            .setHeaders(new DefaultHttpHeaders()
              .add(Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
              .add(AcceptLanguage, "en-US,en;q=0.5")
              .add(AcceptEncoding, "gzip")
              .add(Connection, Close)
              .add(UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0"))
            .setRequestTimeout(1000)

          httpProtocol.proxyPart.proxy.foreach(requestBuilder.setProxyServer)

          try {
            val p = Promise[Unit]
            httpClient.sendRequest(requestBuilder.build(true), 0, true, new HttpListener {
              override def onHttpResponse(httpResponseStatus: HttpResponseStatus, httpHeaders: HttpHeaders): Unit = {}

              override def onThrowable(throwable: Throwable): Unit = p.failure(throwable)

              override def onHttpResponseBodyChunk(byteBuf: ByteBuf, last: Boolean): Unit =
                if (last) {
                  p.success(())
                }
            })
            Await.ready(p.future, 2 seconds)
          } catch {
            case NonFatal(e) =>
              if (logger.underlying.isDebugEnabled)
                logger.debug(s"Couldn't execute warm up request $url", e)
              else
                logger.info(s"Couldn't execute warm up request $url: ${e.detailedMessage}")
          }

        case _ =>
          val expression = "foo".expressionSuccess

          Http(expression)
            .get(expression)
            .header("bar", expression)
            .queryParam(expression, expression)
            .build(coreComponents, httpComponents, throttled = false)

          Http(expression)
            .post(expression)
            .header("bar", expression)
            .formParam(expression, expression)
            .build(coreComponents, httpComponents, throttled = false)
      }

      logger.info("Warm up done")
    }
}
