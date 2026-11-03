/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import java.nio.charset.StandardCharsets.UTF_8
import java.util.function.{ Function => JFunction }

import io.gatling.core.EmptySession
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.cache.Http2PriorKnowledgeSupport
import io.gatling.http.client.{ Http2FlowControlWindows, Http2PriorKnowledge, Request, RequestBuilder }
import io.gatling.http.client.body.string.StringRequestBodyBuilder
import io.gatling.http.client.proxy.HttpProxyServer
import io.gatling.http.client.realm.DigestRealm
import io.gatling.http.client.uri.Uri
import io.gatling.http.protocol.{ HttpProtocol, HttpProtocolBuilder }

import io.netty.handler.codec.http.{ DefaultHttpHeaders, EmptyHttpHeaders, HttpHeaderNames, HttpHeaderValues, HttpMethod, HttpResponseStatus }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class FollowUpProcessorSpec extends AnyFlatSpecLike with Matchers with EmptySession {
  private val configuration = GatlingConfiguration.loadForTest()

  private val httpProtocol: HttpProtocol =
    HttpProtocolBuilder(configuration).enableHttp2
      .http2PriorKnowledge(Map("other.gatling.io" -> true))
      .build

  private val session = Http2PriorKnowledgeSupport.setHttp2PriorKnowledge(httpProtocol)(emptySession)

  private val windows = new Http2FlowControlWindows(1024 * 1024, 10 * 1024 * 1024)
  private val signatureCalculator: JFunction[Request, Request] = request => request
  private val proxy = new HttpProxyServer("localhost", 8080, null, false, EmptyHttpHeaders.INSTANCE)

  private def originalRequest(): Request =
    new RequestBuilder("original", HttpMethod.POST, Uri.create("https://gatling.io/foo"), null)
      .setHeaders(
        new DefaultHttpHeaders()
          .add("X-Custom", "foo")
          .add(HttpHeaderNames.COOKIE, "stale=cookie")
          .add(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE)
      )
      .setBodyBuilder(new StringRequestBodyBuilder("Hello World"))
      .setAutoOrigin(true)
      .setSignatureCalculator(signatureCalculator)
      .setProxyServer(proxy)
      .setHttp2Enabled(true)
      .setHttp2FlowControlWindows(windows)
      .build

  private def keepsOriginalSettings(followUp: Request): Unit = {
    followUp.isAutoOrigin shouldBe true
    followUp.getSignatureCalculator shouldBe theSameInstanceAs(signatureCalculator)
    followUp.getProxyServer shouldBe theSameInstanceAs(proxy)
    followUp.isHttp2Enabled shouldBe true
    followUp.getHttp2FlowControlWindows shouldBe windows
    followUp.getHeaders.get("X-Custom") shouldBe "foo"
    followUp.getHeaders.contains(HttpHeaderNames.COOKIE) shouldBe false
  }

  private def keepsOriginalRequestUnchanged(original: Request, originalHeaders: Map[String, String]): Unit =
    withClue("the original request must not be altered: ") {
      headersOf(original) shouldBe originalHeaders
    }

  private def headersOf(request: Request): Map[String, String] = {
    val headers = request.getHeaders.entries.iterator
    Iterator.continually(headers).takeWhile(_.hasNext).map(_.next()).map(e => e.getKey -> e.getValue).toMap
  }

  "redirectRequest" should "keep the original request's settings and use the redirect remote's prior knowledge" in {
    val original = originalRequest()
    val originalHeaders = headersOf(original)

    val redirect = FollowUpProcessor.redirectRequest(
      "original",
      original,
      session,
      HttpResponseStatus.TEMPORARY_REDIRECT,
      httpProtocol,
      Uri.create("https://other.gatling.io/bar"),
      UTF_8,
      1
    )

    keepsOriginalSettings(redirect)
    redirect.getHttp2PriorKnowledge shouldBe Http2PriorKnowledge.HTTP2_SUPPORTED
    redirect.getHeaders.get(HttpHeaderNames.HOST) shouldBe "other.gatling.io"
    keepsOriginalRequestUnchanged(original, originalHeaders)
  }

  "digestChallengeRequest" should "keep the original request's settings, including the proxy" in {
    val original = originalRequest()
    val originalHeaders = headersOf(original)

    val challenge = FollowUpProcessor.digestChallengeRequest("original", original, session, httpProtocol, new DigestRealm("user", "password"), UTF_8)

    keepsOriginalSettings(challenge)
    challenge.getHttp2PriorKnowledge shouldBe null
    keepsOriginalRequestUnchanged(original, originalHeaders)
  }

  "expectationFailedRequest" should "keep the original request's settings without the expectation" in {
    val original = originalRequest()
    val originalHeaders = headersOf(original)

    val retry = FollowUpProcessor.expectationFailedRequest(original, session, httpProtocol, UTF_8)

    keepsOriginalSettings(retry)
    retry.getName shouldBe "original Retry Without Expect"
    retry.getHeaders.contains(HttpHeaderNames.EXPECT) shouldBe false
    keepsOriginalRequestUnchanged(original, originalHeaders)
  }
}
