/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import java.nio.charset.StandardCharsets
import javax.activation.MimetypesFileTypeMap

import akka.actor.ActorRef
import io.gatling.core.controller.throttle.Throttler
import org.jboss.netty.buffer.ChannelBuffers
import org.scalatest.BeforeAndAfter

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.Try

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.cookie._

import io.gatling.AkkaSpec
import io.gatling.core.config.Protocols
import io.gatling.core.pause.Constant
import io.gatling.core.result.writer.StatsEngine
import io.gatling.core.session.Session
import io.gatling.core.structure.{ ScenarioContext, ScenarioBuilder }
import io.gatling.core.util.Io
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.config._

abstract class HttpSpec extends AkkaSpec with BeforeAndAfter {

  type ChannelProcessor = ChannelHandlerContext => Unit
  type Handler = PartialFunction[DefaultHttpRequest, ChannelProcessor]

  val mockHttpPort = Try(Io.withCloseable(new ServerSocket(0))(_.getLocalPort)).getOrElse(8072)

  def httpProtocol(implicit httpProtocol: DefaultHttpProtocol) =
    new HttpProtocolBuilder(httpProtocol.value).baseURL(s"http://localhost:$mockHttpPort")

  private def newResponse(status: HttpResponseStatus) =
    new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)

  def runWithHttpServer(requestHandler: Handler)(f: HttpServer => Unit)(implicit httpEngine: HttpEngine, protocol: DefaultHttpProtocol) = {
    val httpServer = new HttpServer(requestHandler, mockHttpPort)
    try {
      httpEngine.start(system, mock[StatsEngine], mock[Throttler])
      f(httpServer)
    } finally {
      httpServer.stop()
    }
  }

  def runScenario(sb: ScenarioBuilder,
                  timeout: FiniteDuration = 10.seconds,
                  protocolCustomizer: HttpProtocolBuilder => HttpProtocolBuilder = identity)(implicit defaultHttpProtocol: DefaultHttpProtocol) = {
    val protocols = Protocols(protocolCustomizer(httpProtocol))
    val actor = sb.build(system, self, ScenarioContext(mock[ActorRef], mock[StatsEngine], mock[ActorRef], protocols, Constant, throttled = false))
    actor ! Session("TestSession", 0)
    expectMsgClass(timeout, classOf[Session])
  }

  def sendResponse(content: String = "",
                   status: HttpResponseStatus = HttpResponseStatus.OK,
                   headers: Map[String, String] = Map.empty): ChannelProcessor = ctx => {
    val response = newResponse(status)
    if (content.nonEmpty) response.setContent(ChannelBuffers.copiedBuffer(content, StandardCharsets.UTF_8))
    headers.foreach { case (k, v) => response.headers().add(k, v) }
    sendToChannel(ctx, response)
  }

  // NOTE : Content Type setting through MimetypesFileTypeMap is buggy until JDK8.
  // In case Content Type setting fails under JDK < 8, amend mime.types to add the necessary mappings.
  def sendFile(name: String): ChannelProcessor = ctx => {
    val response = newResponse(HttpResponseStatus.OK)
    val mimeTypesMap = new MimetypesFileTypeMap()

    val resource = getClass.getClassLoader.getResource(name)
    val file = resource.getFile
    val raf = new RandomAccessFile(file, "r")
    val region = new DefaultFileRegion(raf.getChannel, 0, raf.length()) // THIS WORKS ONLY WITH HTTP, NOT HTTPS

    response.headers.set(HeaderNames.ContentType, mimeTypesMap.getContentType(file))

    sendToChannel(ctx, response, region)
  }

  private def sendToChannel(ctx: ChannelHandlerContext, objs: Any*) = {
    val future = Channels.future(ctx.getChannel)
    // Using the same future for multiple writes *may* be wrong
    objs.foreach(obj => Channels.write(ctx, future, obj))
    future.addListener(ChannelFutureListener.CLOSE)
  }

  // Assertions

  def verifyRequestTo(path: String)(implicit server: HttpServer): Unit = verifyRequestTo(path, 1)

  def verifyRequestTo(path: String, count: Int, checks: (DefaultHttpRequest => Unit)*)(implicit server: HttpServer): Unit = {
    val filteredRequests = server.requests.filter(_.getUri == path).toList
    val actualCount = filteredRequests.size
    if (actualCount != count) {
      throw new AssertionError(s"Expected to access $path $count times, but actually accessed it $actualCount times.")
    }

    checks.foreach(check => filteredRequests.foreach(check))
  }

  def checkCookie(cookie: String, value: String)(request: DefaultHttpRequest) = {
    val cookies = ServerCookieDecoder.STRICT.decode(request.headers.get(HeaderNames.Cookie)).toList
    val matchingCookies = cookies.filter(_.name == cookie)

    matchingCookies match {
      case Nil =>
        throw new AssertionError(s"In request $request there were no cookies")
      case list =>
        for (cookie <- list) {
          if (cookie.value != value) {
            throw new AssertionError(s"$request: cookie '${cookie.name}', expected: '$value' but was '${cookie.value}'")
          }
        }
    }
  }

  // Extractor for nicer interaction with Scala
  class HttpRequest(val request: DefaultHttpRequest) {
    def isEmpty = request == null
    def get: (HttpMethod, String) = (request.getMethod, request.getUri)
  }

  object HttpRequest {
    def unapply(request: DefaultHttpRequest) = new HttpRequest(request)
  }
}
