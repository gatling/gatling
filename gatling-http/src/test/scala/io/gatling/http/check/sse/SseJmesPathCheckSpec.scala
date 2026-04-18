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

package io.gatling.http.check.sse

import io.gatling.ValidationValues
import io.gatling.core.{ CoreDsl, EmptySession }
import io.gatling.core.check.{ Check, CheckMaterializer, CheckResult }
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers
import io.gatling.http.HttpDsl
import io.gatling.http.action.sse.fsm.ServerSentEvent

import com.fasterxml.jackson.databind.JsonNode
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SseJmesPathCheckSpec extends AnyFlatSpecLike with Matchers with ValidationValues with CoreDsl with HttpDsl with EmptySession {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val materializer: CheckMaterializer[JmesPathCheckType, SseCheck, ServerSentEvent, JsonNode] =
    SseCheckMaterializer.jmesPath(new JsonParsers)

  "jmesPath.find.exists" should "extract from data field directly" in {
    val event = mockServerSentEvent(None, Some("""{"symbol":"AAPL","price":150.25}"""), None, None)
    jmesPath("symbol").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("AAPL"), None)
  }

  it should "extract numeric values" in {
    val event = mockServerSentEvent(None, Some("""{"price":150.25}"""), None, None)
    jmesPath("price").ofType[Double].find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(150.25), None)
  }

  it should "fail when data field is absent" in {
    val event = mockServerSentEvent(Some("heartbeat"), None, None, None)
    jmesPath("foo").find.exists
      .check(event, emptySession, Check.newPreparedCache)
      .failed shouldBe "jmesPath(foo).find.exists preparation crashed: No SSE data field"
  }

  "jmesPath with nested objects" should "extract nested values" in {
    val event = mockServerSentEvent(None, Some("""{"user":{"id":123,"name":"Alice"}}"""), None, None)
    jmesPath("user.name").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("Alice"), None)
  }

  it should "use array indexing" in {
    val event = mockServerSentEvent(None, Some("""{"items":[{"id":1},{"id":2}]}"""), None, None)
    jmesPath("items[0].id").ofType[Int].find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(1), None)
  }

  "jmesPath with filters" should "filter array elements" in {
    val event = mockServerSentEvent(None, Some("""{"prices":[{"value":100},{"value":200},{"value":300}]}"""), None, None)
    jmesPath("prices[?value > `150`].value").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some("[200,300]"),
      None
    )
  }

  "jmesPath.findAll.exists" should "extract all matches" in {
    val event = mockServerSentEvent(None, Some("""{"users":[{"name":"Alice"},{"name":"Bob"}]}"""), None, None)
    jmesPath("users[*].name").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("""["Alice","Bob"]"""), None)
  }

  "jmesPath.saveAs" should "save extracted value to session" in {
    val event = mockServerSentEvent(None, Some("""{"userId":12345}"""), None, None)
    jmesPath("userId").ofType[Int].saveAs("uid").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(12345), Some("uid"))
  }
}
