/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.render.template

import java.nio.file.Path

import io.gatling.recorder.cli.RecorderArgs
import io.gatling.recorder.config.ConfigKeys.http.{ UseMethodAndUriAsPostfix, UseSimulationAsPrefix }
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.render.{ RequestBodyParams, RequestElement }

import io.netty.handler.codec.http.EmptyHttpHeaders
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class RequestTemplateSpec extends AnyFlatSpecLike with Matchers {
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  private def recorderConfig(extra: Map[String, _ <: Any] = Map.empty): RecorderConfiguration =
    RecorderConfiguration.testConfig(
      RecorderArgs(simulationsFolder = Path.of(""), resourcesFolder = Path.of(""), pkg = None, className = None, format = None),
      extra
    )
  private val url = "http://gatling.io/path1/file1"
  private val simulationClass = "Simulation Class"

  private def mockRequestBody(paramName: String, paramValue: String) = RequestBodyParams(List((paramName, paramValue)))
  private def mockRequestElement(paramName: String, paramValue: String) =
    new RequestElement(url, "post", EmptyHttpHeaders.INSTANCE, Some(mockRequestBody(paramName, paramValue)), EmptyHttpHeaders.INSTANCE, None, 200, Nil, Nil)

  "request template" should "not wrap with joinStrings strings shorter than 65535 characters" in {
    val config = recorderConfig()
    val mockedRequest1 = mockRequestElement("name", "short")
    val res1 = new RequestTemplate(Map.empty, Map.empty, config).render(
      simulationClass,
      mockedRequest1,
      ExtractedUris(Seq(mockedRequest1), config.core.format)
    )
    res1 should include(".formParam(\"name\", \"short\")")

    val mockedRequest2 = mockRequestElement("name", "1" * 65534)
    val res2 = new RequestTemplate(Map.empty, Map.empty, config).render(
      simulationClass,
      mockedRequest2,
      ExtractedUris(Seq(mockedRequest2), config.core.format)
    )
    res2 should not include "Seq"
    res2 should not include ".mkString"
  }

  it should "wrap with joinStrings strings with not less than 65535 characters" in {
    val config = recorderConfig()
    val mockedRequest = mockRequestElement("name", "a" * 65535)
    val res = new RequestTemplate(Map.empty, Map.empty, config).render(
      simulationClass,
      mockedRequest,
      ExtractedUris(Seq(mockedRequest), config.core.format)
    )
    res should include("Seq(\"" + "a" * 65534 + "\", \"a\").mkString")
  }

  it should "use request as prefix by default" in {
    val config = recorderConfig()
    val mockedRequest1 = mockRequestElement("name", "short")
    val res1 = new RequestTemplate(Map.empty, Map.empty, config).render(
      simulationClass,
      mockedRequest1,
      ExtractedUris(Seq(mockedRequest1), config.core.format)
    )
    res1 should include("request_0")
  }

  it should "use simulation as prefix when requested" in {
    val mockedRequest1 = mockRequestElement("name", "short")
    implicit val config: RecorderConfiguration = recorderConfig(Map(UseSimulationAsPrefix -> true))
    val res1 = new RequestTemplate(Map.empty, Map.empty, config).render(
      simulationClass,
      mockedRequest1,
      ExtractedUris(Seq(mockedRequest1), config.core.format)
    )
    res1 should include(s"${simulationClass}_0")
    res1 should not include "request_0"
  }

  it should "use method and URI as postfix when requested" in {
    val mockedRequest1 = mockRequestElement("name", "short")
    implicit val config: RecorderConfiguration = recorderConfig(Map(UseMethodAndUriAsPostfix -> true))
    val res1 = new RequestTemplate(Map.empty, Map.empty, config).render(
      simulationClass,
      mockedRequest1,
      ExtractedUris(Seq(mockedRequest1), config.core.format)
    )
    res1 should include("request_0:post_http://gatling.io/path1/file1")
  }

  it should "escape bad characters in request postfix" in {
    val postfix = RequestTemplate.sanitizeRequestPostfix("POST_https://gatling.io/hello?to=\"john\\doe\"&hobbies={a,b;c\\d}")
    postfix should equal("POST_https://gatling.io/hello?to=_john_doe_&hobbies=_a_b_c_d_")
  }
}
