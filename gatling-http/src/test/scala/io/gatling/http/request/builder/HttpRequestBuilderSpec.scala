/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder

import org.mockito.Mockito._
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.mock.MockitoSugar

import com.ning.http.client.uri.Uri
import com.ning.http.client.{ Request, RequestBuilderBase, SignatureCalculator }

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.validation.Success
import io.gatling.http.config.HttpProtocol

class HttpRequestBuilderSpec extends FlatSpec with Matchers with MockitoSugar {

  // Default config
  GatlingConfiguration.setUpForTest()

  def mockComonAttributes() = CommonAttributes(_ => Success("attributes"), "method", Right(Uri.create("http://gatling-tool.org")))

  "request builder" should "set signature calculator object" in {
    var builder = new HttpRequestBuilder(mockComonAttributes(), HttpAttributes())
    val sigCalc = mock[SignatureCalculator]
    builder = builder.signatureCalculator(sigCalc)

    val httpRequest = builder.build(HttpProtocol.DefaultHttpProtocol, throttled = false)
    httpRequest.signatureCalculator.get shouldBe sigCalc
  }

  it should "set signature calculator function" in {
    var builder = new HttpRequestBuilder(mockComonAttributes(), HttpAttributes())
    val sigCalcFunc = mock[(Request, RequestBuilderBase[_]) => Unit]
    builder = builder.signatureCalculator(sigCalcFunc)

    val httpRequest = builder.build(HttpProtocol.DefaultHttpProtocol, throttled = false)
    val sigCalc = httpRequest.signatureCalculator.get

    val mockRequest = mock[Request]
    val mockRequestBuilder = mock[RequestBuilderBase[_]]

    sigCalc.calculateAndAddSignature(mockRequest, mockRequestBuilder)
    verify(sigCalcFunc, times(1)).apply(mockRequest, mockRequestBuilder)
  }
}
