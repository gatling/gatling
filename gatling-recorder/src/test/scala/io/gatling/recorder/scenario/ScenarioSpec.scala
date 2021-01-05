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

package io.gatling.recorder.scenario

import scala.collection.mutable
import scala.concurrent.duration._

import io.gatling.BaseSpec
import io.gatling.http.client.uri.Uri
import io.gatling.http.fetch.{ BasicResource, CssResource }
import io.gatling.recorder.config.ConfigKeys.http.{ FollowRedirect, InferHtmlResources }
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.config.RecorderConfiguration.fakeConfig

import io.netty.handler.codec.http.{ DefaultHttpHeaders, EmptyHttpHeaders }
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE

class ScenarioSpec extends BaseSpec {

  private implicit val config: RecorderConfiguration = fakeConfig(mutable.Map(FollowRedirect -> true, InferHtmlResources -> true))

  "Scenario" should "remove HTTP redirection " in {

    val r1 = RequestElement("http://gatling.io/", "GET", EmptyHttpHeaders.INSTANCE, None, EmptyHttpHeaders.INSTANCE, None, 200, Nil, Nil)
    val r2 = RequestElement("http://gatling.io/rn1.html", "GET", EmptyHttpHeaders.INSTANCE, None, EmptyHttpHeaders.INSTANCE, None, 302, Nil, Nil)
    val r3 = RequestElement("http://gatling.io/release-note-1.html", "GET", EmptyHttpHeaders.INSTANCE, None, EmptyHttpHeaders.INSTANCE, None, 200, Nil, Nil)
    val r4 = RequestElement("http://gatling.io/details.html", "GET", EmptyHttpHeaders.INSTANCE, None, EmptyHttpHeaders.INSTANCE, None, 200, Nil, Nil)

    val scn = ScenarioDefinition(
      List(
        TimedScenarioElement(1000, 1500, r1),
        TimedScenarioElement(3000, 3500, r2),
        TimedScenarioElement(5000, 5500, r3),
        TimedScenarioElement(7000, 7500, r4)
      ),
      List.empty
    )
    scn.elements shouldBe List(r1, PauseElement(1500.milliseconds), r2.copy(statusCode = 200), PauseElement(1500.milliseconds), r4)
  }

  it should "filter out embedded resources of HTML documents" in {
    val r1 = RequestElement(
      "http://gatling.io",
      "GET",
      EmptyHttpHeaders.INSTANCE,
      None,
      EmptyHttpHeaders.INSTANCE,
      None,
      200,
      List(CssResource(Uri.create("http://gatling.io/main.css")), BasicResource(Uri.create("http://gatling.io/img.jpg"))),
      Nil
    )
    val r2 = RequestElement("http://gatling.io/main.css", "GET", EmptyHttpHeaders.INSTANCE, None, EmptyHttpHeaders.INSTANCE, None, 200, Nil, Nil)
    val r3 = RequestElement(
      "http://gatling.io/details.html",
      "GET",
      new DefaultHttpHeaders().add(CONTENT_TYPE, "text/html;charset=UTF-8"),
      None,
      EmptyHttpHeaders.INSTANCE,
      None,
      200,
      Nil,
      Nil
    )
    val r4 = RequestElement("http://gatling.io/img.jpg", "GET", EmptyHttpHeaders.INSTANCE, None, EmptyHttpHeaders.INSTANCE, None, 200, Nil, Nil)
    val r5 = RequestElement(
      "http://gatling.io",
      "GET",
      EmptyHttpHeaders.INSTANCE,
      None,
      EmptyHttpHeaders.INSTANCE,
      None,
      200,
      List(CssResource(Uri.create("http://gatling.io/main.css"))),
      Nil
    )
    val r6 = RequestElement("http://gatling.io/main.css", "GET", EmptyHttpHeaders.INSTANCE, None, EmptyHttpHeaders.INSTANCE, None, 200, Nil, Nil)

    val scn = ScenarioDefinition(
      List(
        TimedScenarioElement(1000, 1500, r1),
        TimedScenarioElement(2000, 2001, r2),
        TimedScenarioElement(2000, 2002, r3),
        TimedScenarioElement(2000, 2003, r4),
        TimedScenarioElement(5000, 5001, r5),
        TimedScenarioElement(5005, 5010, r6)
      ),
      Nil
    )
    scn.elements shouldBe List(r1.copy(nonEmbeddedResources = List(r3)), PauseElement(2997.milliseconds), r5)
  }
}
