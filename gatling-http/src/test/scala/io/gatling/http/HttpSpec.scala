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
package io.gatling.http

import java.io.RandomAccessFile
import java.net.ServerSocket
import javax.activation.FileTypeMap

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.Try

import io.gatling.AkkaSpec
import io.gatling.commons.util.Io._
import io.gatling.core.CoreComponents
import io.gatling.core.action.{ Action, ActorDelegatingAction }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.pause.Constant
import io.gatling.core.protocol.{ ProtocolComponentsRegistries, Protocols }
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.{ ScenarioBuilder, ScenarioContext }
import io.gatling.http.protocol.HttpProtocolBuilder

import akka.actor.ActorRef
import org.scalatest.BeforeAndAfter
import io.netty.channel._
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.cookie._

abstract class HttpSpec extends AkkaSpec with BeforeAndAfter {

  type ChannelProcessor = ChannelHandlerContext => Unit
  type Handler = PartialFunction[FullHttpRequest, ChannelProcessor]

  val mockHttpPort = Try(withCloseable(new ServerSocket(0))(_.getLocalPort)).getOrElse(8072)

  def httpProtocol(implicit configuration: GatlingConfiguration) =
    HttpProtocolBuilder(configuration).baseURL(s"http://localhost:$mockHttpPort")

  def runWithHttpServer(requestHandler: Handler)(f: HttpServer => Unit) = {
    val httpServer = new HttpServer(requestHandler, mockHttpPort)
    try {
      f(httpServer)
    } finally {
      httpServer.stop()
    }
  }

  def runScenario(
    sb:                 ScenarioBuilder,
    timeout:            FiniteDuration                             = 10.seconds,
    protocolCustomizer: HttpProtocolBuilder => HttpProtocolBuilder = identity
  )(implicit configuration: GatlingConfiguration) = {
    val protocols = Protocols(protocolCustomizer(httpProtocol))
    val coreComponents = CoreComponents(mock[ActorRef], mock[Throttler], mock[StatsEngine], mock[Action], configuration)
    val protocolComponentsRegistry = new ProtocolComponentsRegistries(system, coreComponents, protocols).scenarioRegistry(Protocols(Nil))
    val next = new ActorDelegatingAction("next", self)
    val actor = sb.build(ScenarioContext(system, coreComponents, protocolComponentsRegistry, Constant, throttled = false), next)
    actor ! Session("TestSession", 0)
    expectMsgClass(timeout, classOf[Session])
  }

  // NOTE : Content Type setting through MimetypesFileTypeMap is buggy until JDK8.
  // In case Content Type setting fails under JDK < 8, amend mime.types to add the necessary mappings.
  def sendFile(name: String): ChannelProcessor = ctx => {
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)

    val resource = getClass.getClassLoader.getResource(name)
    val fileUri = resource.getFile
    val raf = new RandomAccessFile(fileUri, "r")
    val region = new DefaultFileRegion(raf.getChannel, 0, raf.length) // THIS WORKS ONLY WITH HTTP, NOT HTTPS

    response.headers
      .set(HeaderNames.ContentType, FileTypeMap.getDefaultFileTypeMap.getContentType(fileUri))
      .set(HeaderNames.ContentLength, raf.length)

    ctx.write(response)
    ctx.write(region)
    ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
      .addListener(ChannelFutureListener.CLOSE)
  }

  // Assertions

  def verifyRequestTo(path: String)(implicit server: HttpServer): Unit = verifyRequestTo(path, 1)

  def verifyRequestTo(path: String, count: Int, checks: (FullHttpRequest => Unit)*)(implicit server: HttpServer): Unit = {
    val filteredRequests = server.requests.filter(_.getUri == path).toList
    val actualCount = filteredRequests.size
    if (actualCount != count) {
      throw new AssertionError(s"Expected to access $path $count times, but actually accessed it $actualCount times.")
    }

    checks.foreach(check => filteredRequests.foreach(check))
  }

  def checkCookie(cookie: String, value: String)(request: FullHttpRequest) = {
    val cookies = ServerCookieDecoder.STRICT.decode(request.headers.get(HeaderNames.Cookie)).toList
    val matchingCookies = cookies.filter(_.name == cookie)

    matchingCookies match {
      case Nil =>
        throw new AssertionError(s"In request $request there were no cookies")
      case list =>
        for (cookie <- list if cookie.value != value) {
          throw new AssertionError(s"$request: cookie '${cookie.name}', expected: '$value' but was '${cookie.value}'")
        }
    }
  }

  // Extractor for nicer interaction with Scala
  class HttpRequest(val request: FullHttpRequest) {
    def isEmpty = request == null
    def get: (HttpMethod, String) = (request.getMethod, request.getUri)
  }

  object HttpRequest {
    def unapply(request: FullHttpRequest) = new HttpRequest(request)
  }
}
