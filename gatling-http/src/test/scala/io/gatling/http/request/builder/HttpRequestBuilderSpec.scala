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

import scala.jdk.CollectionConverters._

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.commons.util.DefaultClock
import io.gatling.core.CoreComponents
import io.gatling.core.EmptySession
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.http.Predef._
import io.gatling.http.cache.DnsCacheSupport
import io.gatling.http.cache.HttpCaches
import io.gatling.http.check.HttpCheckScope._
import io.gatling.http.client.{ HttpClientConfig, Request, SignatureCalculator }
import io.gatling.http.client.body.form.FormUrlEncodedRequestBody
import io.gatling.http.client.impl.request.WritableRequestBuilder
import io.gatling.http.client.resolver.InetAddressNameResolver
import io.gatling.http.client.uri.Uri
import io.gatling.http.protocol.HttpProtocol

import io.netty.handler.codec.http.HttpMethod

class HttpRequestBuilderSpec extends BaseSpec with ValidationValues with EmptySession {

  // Default config
  private val configuration = GatlingConfiguration.loadForTest()
  private val clock = new DefaultClock
  private val coreComponents = new CoreComponents(null, null, null, null, null, clock, null, configuration)
  private val httpCaches = new HttpCaches(coreComponents)
  private val sessionBase = emptySession.set(DnsCacheSupport.DnsNameResolverAttributeName, InetAddressNameResolver.JAVA_RESOLVER)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  private def httpRequestDef(f: HttpRequestBuilder => HttpRequestBuilder, httpProtocol: HttpProtocol = HttpProtocol(configuration)) = {
    val commonAttributes = CommonAttributes("requestName".expressionSuccess, HttpMethod.GET, Right(Uri.create("http://gatling.io")))
    val builder = f(new HttpRequestBuilder(commonAttributes, HttpAttributes.Empty))
    builder.build(httpCaches, httpProtocol, throttled = false, configuration)
  }

  "signature calculator" should "work when passed as a SignatureCalculator instance" in {
    httpRequestDef(_.sign(new SignatureCalculator {
      override def sign(request: Request): Unit = request.getHeaders.add("X-Token", "foo")
    }.expressionSuccess))
      .build("requestName", sessionBase)
      .map { httpRequest =>
        val writableRequest = WritableRequestBuilder.buildRequest(httpRequest.clientRequest, null, new HttpClientConfig, false)
        writableRequest.getRequest.headers.get("X-Token")
      }
      .succeeded shouldBe "foo"
  }

  "form" should "work when overriding a value" in {

    val form = Map("foo" -> Seq("FOO"), "bar" -> Seq("BAR"))
    val session = sessionBase.set("form", form).set("formParamToOverride", "bar")

    httpRequestDef(_.form("${form}".el).formParam("${formParamToOverride}".el, "BAZ".el))
      .build("requestName", session)
      .map(
        _.clientRequest.getBody.asInstanceOf[FormUrlEncodedRequestBody].getContent.asScala.collect { case param if param.getName == "bar" => param.getValue }
      )
      .succeeded shouldBe Seq("BAZ")
  }

  it should "work when passing only formParams" in {

    val session = sessionBase.set("formParam", "bar")

    httpRequestDef(_.formParam("${formParam}".el, "BAR".el))
      .build("requestName", session)
      .map(
        _.clientRequest.getBody.asInstanceOf[FormUrlEncodedRequestBody].getContent.asScala.collect { case param if param.getName == "bar" => param.getValue }
      )
      .succeeded shouldBe Seq("BAR")
  }

  it should "work when passing only a form" in {

    val form = Map("foo" -> Seq("FOO"), "bar" -> Seq("BAR"))
    val session = sessionBase.set("form", form)

    httpRequestDef(_.form("${form}".el))
      .build("requestName", session)
      .map(
        _.clientRequest.getBody.asInstanceOf[FormUrlEncodedRequestBody].getContent.asScala.collect { case param if param.getName == "bar" => param.getValue }
      )
      .succeeded shouldBe Seq("BAR")
  }

  "checks" should "respect their scope priority" in {

    val result = httpRequestDef(builder => {
      builder
        .check(bodyString.notNull)
        .check(status.is(200))
        .check(currentLocation.is("current location"))
        .check(header("HEADER").is("VALUE"))
        .check(responseTimeInMillis.lt(300))
    }).build("requestName", sessionBase).map(_.requestConfig.checks).succeeded

    result.map(_.scope) shouldBe Seq(
      Url,
      Status,
      Header,
      Body,
      Time
    )
  }

  "checks" should "respect their provenance priority" in {

    val result = httpRequestDef(
      _.check(bodyString.notNull),
      http(configuration)
        .baseUrl("https://gatling.io")
        .check(md5.notNull)
        .build
    ).build("requestName", sessionBase)
      .map(_.requestConfig.checks)
      .succeeded

    result.map(_.scope) shouldBe Seq(
      Status,
      Body,
      Chunks
    )
  }

  "checks" should "respect user defined order on request" in {
    val result = httpRequestDef(builder => {
      builder
        .check(bodyString.notNull)
        .check(md5.notNull)
        .check(bodyString.notNull)
        .check()
    }).build("requestName", sessionBase).map(_.requestConfig.checks).succeeded

    result.map(_.scope) shouldBe Seq(
      Status,
      Body,
      Chunks,
      Body
    )
  }

  "checks" should "respect user defined order on protocol" in {
    val result = httpRequestDef(
      f => f,
      http(configuration)
        .baseUrl("https://gatling.io")
        .check(bodyString.notNull)
        .check(md5.notNull)
        .check(bodyString.notNull)
        .build
    ).build("requestName", sessionBase).map(_.requestConfig.checks).succeeded

    result.map(_.scope) shouldBe Seq(
      Status,
      Body,
      Chunks,
      Body
    )
  }

  // Not possible to check prior of default over protocol, as only default added value is status
  // and the presence of a status (for same scope priority) remove the default one.
}
