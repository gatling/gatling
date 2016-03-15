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

import scala.collection.JavaConversions._

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.http.ahc.{ HttpEngine, ResponseProcessor }
import io.gatling.http.cache.HttpCaches
import io.gatling.http.protocol.{ HttpComponents, HttpProtocol }

import org.asynchttpclient.{ Request, RequestBuilderBase, SignatureCalculator }
import org.asynchttpclient.uri.Uri
import org.mockito.Mockito.when

class HttpRequestBuilderSpec extends BaseSpec with ValidationValues {

  // Default config
  val configuration = GatlingConfiguration.loadForTest()
  val coreComponents = mock[CoreComponents]
  when(coreComponents.configuration).thenReturn(configuration)
  val httpCaches = new HttpCaches(configuration)
  val httpComponents = HttpComponents(HttpProtocol(configuration), mock[HttpEngine], httpCaches, mock[ResponseProcessor])

  def httpRequestDef(f: HttpRequestBuilder => HttpRequestBuilder) = {
    val commonAttributes = CommonAttributes("requestName".expressionSuccess, "GET", Right(Uri.create("http://gatling.io")))
    val builder = f(new HttpRequestBuilder(commonAttributes, HttpAttributes()))
    builder.build(coreComponents, httpComponents, throttled = false)
  }

  "signature calculator" should "work when passed as a SignatureCalculator instance" in {
    val sigCalc = new SignatureCalculator {
      def calculateAndAddSignature(request: Request, rb: RequestBuilderBase[_]): Unit = rb.addHeader("X-Token", "foo")
    }

    httpRequestDef(_.signatureCalculator(sigCalc))
      .build("requestName", Session("scenarioName", 0))
      .map(_.ahcRequest.getHeaders.get("X-Token")).succeeded shouldBe "foo"
  }

  it should "work when passed as a function" in {
      def sigCalc(request: Request, rb: RequestBuilderBase[_]): Unit = rb.addHeader("X-Token", "foo")

    httpRequestDef(_.signatureCalculator(sigCalc _))
      .build("requestName", Session("scenarioName", 0))
      .map(_.ahcRequest.getHeaders.get("X-Token")).succeeded shouldBe "foo"
  }

  "form" should "work when overriding a value" in {

    val form = Map("foo" -> Seq("FOO"), "bar" -> Seq("BAR"))
    val session = Session("scenarioName", 0).set("form", form).set("formParamToOverride", "bar")

    httpRequestDef(_.form("${form}".el).formParam("${formParamToOverride}".el, "BAZ".el))
      .build("requestName", session)
      .map(_.ahcRequest.getFormParams.collect { case param if param.getName == "bar" => param.getValue }).succeeded shouldBe Seq("BAZ")
  }

  it should "work when passing only formParams" in {

    val session = Session("scenarioName", 0).set("formParam", "bar")

    httpRequestDef(_.formParam("${formParam}".el, "BAR".el))
      .build("requestName", session)
      .map(_.ahcRequest.getFormParams.collect { case param if param.getName == "bar" => param.getValue }).succeeded shouldBe Seq("BAR")
  }

  it should "work when passing only a form" in {

    val form = Map("foo" -> Seq("FOO"), "bar" -> Seq("BAR"))
    val session = Session("scenarioName", 0).set("form", form)

    httpRequestDef(_.form("${form}".el))
      .build("requestName", session)
      .map(_.ahcRequest.getFormParams.collect { case param if param.getName == "bar" => param.getValue }).succeeded shouldBe Seq("BAR")
  }
}
