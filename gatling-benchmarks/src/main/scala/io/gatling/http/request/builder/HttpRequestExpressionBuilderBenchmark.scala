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

import io.gatling.commons.validation._
import io.gatling.core.{ CoreComponents, ValidationImplicits }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.cache.HttpCaches
import io.gatling.http.protocol._
import io.gatling.http.client.{ Request => AhcRequest, RequestBuilder => AhcRequesBuilder }
import io.gatling.http.client.ahc.uri.Uri

import com.softwaremill.quicklens._
import io.netty.handler.codec.http.HttpMethod
import org.openjdk.jmh.annotations.Benchmark

object HttpRequestExpressionBuilderBenchmark extends ValidationImplicits {

  private val config = GatlingConfiguration.loadForTest()

  private val coreComponents = CoreComponents(
    system = null,
    controller = null,
    throttler = null,
    statsEngine = null,
    exit = null,
    configuration = config
  )

  private val httpProtocol = HttpProtocolBuilder(config)
    .baseUrl("http://localhost:8000")
    .build

  private val httpCaches = new HttpCaches(config)

  private val httpComponents = HttpComponents(
    httpProtocol = httpProtocol,
    httpEngine = null,
    httpCaches = httpCaches,
    responseProcessor = null
  )

  val Reference: Expression[AhcRequest] = _ =>
    new AhcRequesBuilder(HttpMethod.GET, Uri.create("http://localhost:8000/ping"))
      .build(true).success

  val RequestWithStaticAbsoluteUrl: Expression[AhcRequest] =
    new Http("requestName").get("http://localhost:8000/ping")
      .build(coreComponents, httpComponents, throttled = false).clientRequest

  val RequestWithStaticRelativeUrl: Expression[AhcRequest] =
    new Http("requestName").get("/ping")
      .build(coreComponents, httpComponents, throttled = false).clientRequest

  val RequestWithStaticQueryParams: Expression[AhcRequest] =
    new Http("requestName").get("/ping")
      .queryParam("hello", "world")
      .queryParam("foo", "bar")
      .build(coreComponents, httpComponents, throttled = false).clientRequest

  val RequestWithDynamicQuery: Expression[AhcRequest] =
    new Http("requestName").get("/ping?foo=${key}")
      .build(coreComponents, httpComponents, throttled = false).clientRequest

  val RequestWithStaticHeaders: Expression[AhcRequest] = {

    val httpProtocol = HttpProtocolBuilder(config)
      .baseUrl("http://localhost:8000")
      .acceptEncodingHeader("gzip, deflate")
      .acceptLanguageHeader("en-GB,en;q=0.5")
      .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:35.0) Gecko/20100101 Firefox/35.0")
      .build

    new Http("requestName").get("/ping")
      .build(
        coreComponents,
        httpComponents.modify(_.httpProtocol).setTo(httpProtocol),
        throttled = false
      ).clientRequest
  }

  val EmptySession: Session = Session("scenario", 0)

  val NonEmptySession: Session = Session("scenario", 0, attributes = Map("key" -> "value"))
}

class HttpRequestExpressionBuilderBenchmark {

  import HttpRequestExpressionBuilderBenchmark._

  @Benchmark
  def testReference(): Validation[AhcRequest] =
    Reference(EmptySession)

  @Benchmark
  def testRequestWithStaticRelativeUrl(): Validation[AhcRequest] =
    RequestWithStaticRelativeUrl(EmptySession)

  @Benchmark
  def testRequestWithStaticAbsoluteUrl(): Validation[AhcRequest] =
    RequestWithStaticAbsoluteUrl(EmptySession)

  @Benchmark
  def testRequestWithStaticQueryParams(): Validation[AhcRequest] =
    RequestWithStaticQueryParams(EmptySession)

  @Benchmark
  def testRequestWithStaticHeaders(): Validation[AhcRequest] =
    RequestWithStaticHeaders(EmptySession)

  @Benchmark
  def testRequestWithDynamicQuery(): Validation[AhcRequest] =
    RequestWithDynamicQuery(NonEmptySession)
}

object Test {
  def main(args: Array[String]): Unit = {
    val test = new HttpRequestExpressionBuilderBenchmark

    System.out.println("testReference=" + test.testReference())
    System.out.println("testRequestWithStaticRelativeUrl=" + test.testRequestWithStaticRelativeUrl())
    System.out.println("testRequestWithStaticAbsoluteUrl" + test.testRequestWithStaticAbsoluteUrl())
    System.out.println("testRequestWithStaticQueryParams=" + test.testRequestWithStaticQueryParams())
    System.out.println("testRequestWithStaticHeaders=" + test.testRequestWithStaticHeaders())
    System.out.println("testRequestWithDynamicQuery=" + test.testRequestWithDynamicQuery())
  }
}
