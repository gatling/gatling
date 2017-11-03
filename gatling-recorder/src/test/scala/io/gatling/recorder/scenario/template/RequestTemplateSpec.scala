/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.recorder.scenario.template

import io.gatling.BaseSpec
import io.gatling.recorder.scenario.{ RequestBodyParams, RequestElement }

class RequestTemplateSpec extends BaseSpec {

  val url = "http://gatling.io/path1/file1"
  val simulationClass = "Simulation Class"

  def mockRequestBody(paramName: String, paramValue: String) = RequestBodyParams(List((paramName, paramValue)))
  def mockRequestElement(paramName: String, paramValue: String) =
    RequestElement(url, "post", Map(), Some(mockRequestBody(paramName, paramValue)), None, 200, Nil)

  "request template" should "not wrap with joinStrings strings shorter than 65535 characters" in {
    val mockedRequest1 = mockRequestElement("name", "short")
    val res1 = RequestTemplate.render(simulationClass, mockedRequest1, new ExtractedUris(Seq(mockedRequest1)))
    res1 should include(".formParam(\"name\", \"short\")")

    val mockedRequest2 = mockRequestElement("name", "1" * 65534)
    val res2 = RequestTemplate.render(simulationClass, mockedRequest2, new ExtractedUris(Seq(mockedRequest2)))
    res2 should not include "Seq"
    res2 should not include ".mkString"
  }

  it should "wrap with joinStrings strings with not less than 65535 characters" in {
    val mockedRequest = mockRequestElement("name", "a" * 65535)
    val res = RequestTemplate.render(simulationClass, mockedRequest, new ExtractedUris(Seq(mockedRequest)))
    res should include("Seq(\"" + "a" * 65534 + "\", \"a\").mkString")
  }
}
