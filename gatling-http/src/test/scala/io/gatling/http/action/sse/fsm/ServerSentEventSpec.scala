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

package io.gatling.http.action.sse.fsm

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class ServerSentEventSpec extends AnyFlatSpecLike with Matchers {

  "asJsonString" should "inject JSON object data as-is" in {
    ServerSentEvent(
      event = Option("EVENT"),
      data = Option("""{"foo": "bar"}"""),
      id = Option("ID"),
      retry = Option(1)
    ).asJsonString shouldBe """{"event":"EVENT","id":"ID","data":{"foo": "bar"},"retry":1}"""
  }

  "asJsonString" should "inject JSON array data as-is" in {
    ServerSentEvent(
      event = Option("EVENT"),
      data = Option("""[{"foo": "bar"}]"""),
      id = Option("ID"),
      retry = Option(1)
    ).asJsonString shouldBe """{"event":"EVENT","id":"ID","data":[{"foo": "bar"}],"retry":1}"""
  }

  it should "escape non-JSON text data" in {
    ServerSentEvent(
      event = Option("EVENT"),
      data = Option("DATA"),
      id = Option("ID"),
      retry = Option(1)
    ).asJsonString shouldBe """{"event":"EVENT","id":"ID","data":"DATA","retry":1}"""
  }
}
