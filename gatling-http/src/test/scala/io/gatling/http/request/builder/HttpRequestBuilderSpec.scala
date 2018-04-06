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

import scala.collection.JavaConverters._

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.http.cache.HttpCaches
import io.gatling.http.client.{ HttpClientConfig, SignatureCalculator }
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.client.body.{ FormUrlEncodedRequestBody, RequestBody }
import io.gatling.http.client.impl.request.WritableRequestBuilder
import io.gatling.http.engine.{ HttpEngine, ResponseProcessor }
import io.gatling.http.protocol.{ HttpComponents, HttpProtocol }

import akka.actor.ActorSystem
import io.netty.handler.codec.http.{ HttpHeaders, HttpMethod }
import org.mockito.Mockito.when

class HttpRequestBuilderSpec extends BaseSpec with ValidationValues {

  // Default config
  val configuration = GatlingConfiguration.loadForTest()
  val coreComponents = mock[CoreComponents]
  when(coreComponents.configuration).thenReturn(configuration)
  when(coreComponents.system).thenReturn(mock[ActorSystem])
  val httpEngine = mock[HttpEngine]
  when(httpEngine.coreComponents).thenReturn(coreComponents)
  val httpCaches = new HttpCaches(configuration)
  val httpComponents = HttpComponents(HttpProtocol(configuration), httpEngine, httpCaches, mock[ResponseProcessor])

  def httpRequestDef(f: HttpRequestBuilder => HttpRequestBuilder) = {
    val commonAttributes = CommonAttributes("requestName".expressionSuccess, HttpMethod.GET, Right(Uri.create("http://gatling.io")))
    val builder = f(new HttpRequestBuilder(commonAttributes, HttpAttributes()))
    builder.build(coreComponents, httpComponents, throttled = false)
  }

  "signature calculator" should "work when passed as a SignatureCalculator instance" in {
    val sigCalc = new SignatureCalculator {
      override def sign(method: HttpMethod, uri: Uri, headers: HttpHeaders, body: RequestBody[_]): Unit = headers.add("X-Token", "foo")
    }

    httpRequestDef(_.signatureCalculator(sigCalc))
      .build("requestName", Session("scenarioName", 0))
      .map { httpRequest =>
        val writableRequest = WritableRequestBuilder.buildRequest(httpRequest.clientRequest, null, new HttpClientConfig)
        writableRequest.getRequest.headers.get("X-Token")
      }.succeeded shouldBe "foo"
  }

  it should "work when passed as a function" in {
    def sigCalc(method: HttpMethod, uri: Uri, headers: HttpHeaders, body: RequestBody[_]): Unit = headers.add("X-Token", "foo")

    httpRequestDef(_.signatureCalculator(sigCalc _))
      .build("requestName", Session("scenarioName", 0))
      .map { httpRequest =>
        val writableRequest = WritableRequestBuilder.buildRequest(httpRequest.clientRequest, null, new HttpClientConfig)
        writableRequest.getRequest.headers.get("X-Token")
      }.succeeded shouldBe "foo"
  }

  "form" should "work when overriding a value" in {

    val form = Map("foo" -> Seq("FOO"), "bar" -> Seq("BAR"))
    val session = Session("scenarioName", 0).set("form", form).set("formParamToOverride", "bar")

    httpRequestDef(_.form("${form}".el).formParam("${formParamToOverride}".el, "BAZ".el))
      .build("requestName", session)
      .map(_.clientRequest.getBody.asInstanceOf[FormUrlEncodedRequestBody].getContent.asScala.collect { case param if param.getName == "bar" => param.getValue }).succeeded shouldBe Seq("BAZ")
  }

  it should "work when passing only formParams" in {

    val session = Session("scenarioName", 0).set("formParam", "bar")

    httpRequestDef(_.formParam("${formParam}".el, "BAR".el))
      .build("requestName", session)
      .map(_.clientRequest.getBody.asInstanceOf[FormUrlEncodedRequestBody].getContent.asScala.collect { case param if param.getName == "bar" => param.getValue }).succeeded shouldBe Seq("BAR")
  }

  it should "work when passing only a form" in {

    val form = Map("foo" -> Seq("FOO"), "bar" -> Seq("BAR"))
    val session = Session("scenarioName", 0).set("form", form)

    httpRequestDef(_.form("${form}".el))
      .build("requestName", session)
      .map(_.clientRequest.getBody.asInstanceOf[FormUrlEncodedRequestBody].getContent.asScala.collect { case param if param.getName == "bar" => param.getValue }).succeeded shouldBe Seq("BAR")
  }
}
