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
package io.gatling.http.request.builder

import com.ning.http.client.uri.Uri
import com.ning.http.client.{ Request, RequestBuilderBase, SignatureCalculator }

import io.gatling.BaseSpec
import io.gatling.core.ValidationValues
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.cache.HttpCaches
import io.gatling.http.config.DefaultHttpProtocol

class HttpRequestBuilderSpec extends BaseSpec with ValidationValues {

  // Default config
  implicit val configuration = GatlingConfiguration.loadForTest()
  implicit val httpEngine = mock[HttpEngine]
  implicit val httpCaches = new HttpCaches
  val defaultHttpProtocol = new DefaultHttpProtocol().value

  private def performTest(addSignatureCalculator: HttpRequestBuilder => HttpRequestBuilder): Unit = {

    val commonAttributes = CommonAttributes("requestName".expression, "GET", Right(Uri.create("http://gatling.io")))

    val builder = addSignatureCalculator(new HttpRequestBuilder(commonAttributes, HttpAttributes()))

    val httpRequestDef = builder.build(defaultHttpProtocol, throttled = false)
    httpRequestDef.build("requestName", Session("scenarioName", 0)).map(_.ahcRequest.getHeaders.getFirstValue("X-Token")).succeeded shouldBe "foo"
  }

  "request builder" should "set signature calculator object" in {
    val sigCalc = new SignatureCalculator {
      def calculateAndAddSignature(request: Request, rb: RequestBuilderBase[_]): Unit = rb.addHeader("X-Token", "foo")
    }

    performTest(_.signatureCalculator(sigCalc))
  }

  it should "set signature calculator function" in {
      def sigCalc(request: Request, rb: RequestBuilderBase[_]): Unit = rb.addHeader("X-Token", "foo")

    performTest(_.signatureCalculator(sigCalc _))
  }
}
