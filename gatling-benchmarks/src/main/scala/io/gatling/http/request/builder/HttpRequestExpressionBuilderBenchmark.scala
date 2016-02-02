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

import io.gatling.commons.validation.Validation
import io.gatling.core.{ ValidationImplicits, CoreComponents }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.cache.HttpCaches
import io.gatling.http.protocol._

import org.asynchttpclient.Request
import org.openjdk.jmh.annotations.Benchmark

object HttpRequestExpressionBuilderBenchmark extends ValidationImplicits {

  private val config = GatlingConfiguration.loadForTest()

  private val coreComponents = CoreComponents(
    controller = null,
    throttler = null,
    statsEngine = null,
    exit = null,
    configuration = config
  )

  private val httpProtocol = HttpProtocolBuilder(config)
    .baseURL("http://localhost:8000")
    .build

  private val httpCaches = new HttpCaches(config)

  private val httpComponents = HttpComponents(
    httpProtocol = httpProtocol,
    httpEngine = null,
    httpCaches = httpCaches
  )

  val SimpleRequest: Expression[Request] =
    new Http("requestName").get("/ping")
      .build(coreComponents, httpComponents, throttled = false).ahcRequest

  val RequestWithHeaders: Expression[Request] =
    new Http("requestName").get("/ping")
      .queryParam("hello", "world")
      .build(coreComponents, httpComponents, throttled = false).ahcRequest

  val EmptySession: Session = Session("scenario", 0)
}

class HttpRequestExpressionBuilderBenchmark {

  import HttpRequestExpressionBuilderBenchmark._

  //  @Benchmark
  //  def testSimpleRequest(): Validation[Request] =
  //    SimpleRequest(EmptySession)

  @Benchmark
  def testRequestWithHeaders(): Validation[Request] =
    RequestWithHeaders(EmptySession)
}
