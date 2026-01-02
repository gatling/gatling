/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.core.body

import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.ValidationValues
import io.gatling.core.EmptySession
import io.gatling.core.session._

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class PebbleFileBodySpec extends AnyFlatSpecLike with Matchers with ValidationValues with EmptySession {
  private val pebbleFileBodies: PebbleFileBodies = new PebbleFileBodies(Long.MaxValue)

  "PebbleFileBody" should "support templates inheritance" in {
    val session = emptySession.set("name", "Mitchell")
    val body = PebbleFileBody("pebble/home.html".expressionSuccess, pebbleFileBodies, UTF_8)
    body(session).succeeded shouldBe """<html>
                                       |<head>
                                       |  <title>Home</title>
                                       |</head>
                                       |<body>
                                       |<div id="content">
                                       |  <h1>Home</h1>
                                       |<p>Welcome to my home page. My name is Mitchell.</p>
                                       |</div>
                                       |<div id="footer">
                                       |  Copyright 2018
                                       |</div>
                                       |</body>
                                       |</html>""".stripMargin
  }
}
