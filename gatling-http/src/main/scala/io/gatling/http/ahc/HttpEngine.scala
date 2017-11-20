/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

package io.gatling.http.ahc

import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.core.CoreComponents
import io.gatling.core.session._
import io.gatling.core.util.NameGen
import io.gatling.http.HeaderNames._
import io.gatling.http.HeaderValues._
import io.gatling.http.fetch.ResourceFetcher
import io.gatling.http.protocol.{ HttpComponents, HttpProtocol }
import io.gatling.http.request.builder.Http
import io.gatling.http.resolver.ExtendedDnsNameResolver
import io.gatling.http.util.HttpTypeCaster

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.asynchttpclient.{ AsyncHttpClient, RequestBuilder }

object HttpEngine {
  private val AhcAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.ahc"

  def apply(system: ActorSystem, coreComponents: CoreComponents): HttpEngine =
    new HttpEngine(system, coreComponents, AhcFactory(system, coreComponents))
}

class HttpEngine(
    system:                       ActorSystem,
    protected val coreComponents: CoreComponents,
    ahcFactory:                   AhcFactory
)
  extends ResourceFetcher with NameGen with StrictLogging {

  def defaultDnsNameResolver: ExtendedDnsNameResolver = ahcFactory.defaultDnsNameResolver

  def httpClient(session: Session, httpProtocol: HttpProtocol): (Session, AsyncHttpClient) =
    if (httpProtocol.enginePart.shareClient) {
      (session, ahcFactory.defaultAhc)

    } else {
      // import optimized TypeCaster
      import HttpTypeCaster._
      session(HttpEngine.AhcAttributeName).asOption[AsyncHttpClient] match {
        case Some(client) => (session, client)
        case _ =>
          val httpClient = ahcFactory.newAhc(session)
          (session.set(HttpEngine.AhcAttributeName, httpClient), httpClient)
      }
    }

  private[this] var warmedUp = false

  def warmpUp(httpComponents: HttpComponents): Unit =
    if (!warmedUp) {
      logger.info("Start warm up")
      warmedUp = true

      import httpComponents._

      if (httpProtocol.enginePart.perUserNameResolution) {
        // eager load
        val _ = defaultDnsNameResolver
      }

      httpProtocol.warmUpUrl match {
        case Some(url) =>
          val requestBuilder = new RequestBuilder().setUrl(url)
            .setHeader(Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .setHeader(AcceptLanguage, "en-US,en;q=0.5")
            .setHeader(AcceptEncoding, "gzip")
            .setHeader(Connection, KeepAlive)
            .setHeader(UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
            .setRequestTimeout(1000)

          httpProtocol.proxyPart.proxy.foreach(requestBuilder.setProxyServer)

          try {
            ahcFactory.defaultAhc.executeRequest(requestBuilder.build).get
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
