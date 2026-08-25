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
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers
import io.gatling.http.HttpDsl
import io.gatling.http.action.sse.fsm.ServerSentEvent

import com.fasterxml.jackson.databind.JsonNode
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SseJsonPathCheckSpec extends AnyFlatSpecLike with Matchers with ValidationValues with CoreDsl with HttpDsl with EmptySession {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val materializer: CheckMaterializer[JsonPathCheckType, SseCheck, ServerSentEvent, JsonNode] =
    SseCheckMaterializer.jsonPath(new JsonParsers)

  "jsonPath.find.exists" should "extract from data field directly" in {
    val event = mockServerSentEvent(None, Some("""{"symbol":"AAPL","price":150.25}"""), None, None)
    jsonPath("$.symbol").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("AAPL"), None)
  }

  it should "extract numeric values" in {
    val event = mockServerSentEvent(None, Some("""{"price":150.25}"""), None, None)
    jsonPath("$.price").ofType[Double].find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(150.25), None)
  }

  it should "fail when data field is absent" in {
    val event = mockServerSentEvent(Some("heartbeat"), None, None, None)
    jsonPath("$.foo").find.exists
      .check(event, emptySession, Check.newPreparedCache)
      .failed shouldBe "jsonPath($.foo).find.exists preparation crashed: No SSE data field"
  }

  it should "fail when data is not valid JSON" in {
    val event = mockServerSentEvent(None, Some("not json"), None, None)
    jsonPath("$.foo").find.exists.check(event, emptySession, Check.newPreparedCache).failed should include("Unrecognized token 'not'")
  }

  "jsonPath.find.exists with nested objects" should "extract nested values" in {
    val event = mockServerSentEvent(None, Some("""{"user":{"id":123,"name":"Alice"}}"""), None, None)
    jsonPath("$.user.name").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("Alice"), None)
  }

  it should "extract from arrays" in {
    val event = mockServerSentEvent(None, Some("""{"items":[{"id":1},{"id":2}]}"""), None, None)
    jsonPath("$.items[0].id").ofType[Int].find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(1), None)
  }

  "jsonPath.findAll.exists" should "extract all matches" in {
    val event = mockServerSentEvent(None, Some("""{"prices":[100,200,300]}"""), None, None)
    jsonPath("$.prices[*]").ofType[Int].findAll.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Seq(100, 200, 300)),
      None
    )
  }

  it should "extract nested array elements" in {
    val event = mockServerSentEvent(None, Some("""{"users":[{"name":"Alice"},{"name":"Bob"}]}"""), None, None)
    jsonPath("$.users[*].name").findAll.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Seq("Alice", "Bob")),
      None
    )
  }

  "jsonPath.count.exists" should "count matching elements" in {
    val event = mockServerSentEvent(None, Some("""{"items":[1,2,3,4,5]}"""), None, None)
    jsonPath("$.items[*]").count.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(5), None)
  }

  "jsonPath with null values" should "handle null correctly" in {
    val event = mockServerSentEvent(None, Some("""{"value":null}"""), None, None)
    jsonPath("$.value").ofType[String].find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "succeed when expecting null" in {
    val event = mockServerSentEvent(None, Some("""{"value":null}"""), None, None)
    jsonPath("$.value").ofType[Any].find.isNull.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "fail when expecting null but getting value" in {
    val event = mockServerSentEvent(None, Some("""{"value":"something"}"""), None, None)
    jsonPath("$.value")
      .ofType[Any]
      .find
      .isNull
      .check(event, emptySession, Check.newPreparedCache)
      .failed shouldBe "jsonPath($.value).find.isNull, found something"
  }

  "jsonPath.saveAs" should "save extracted value to session" in {
    val event = mockServerSentEvent(None, Some("""{"userId":12345}"""), None, None)
    jsonPath("$.userId").ofType[Int].saveAs("uid").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(12345), Some("uid"))
  }

  "jsonPath with complex data" should "handle real-world SSE message" in {
    val event = mockServerSentEvent(Some("price_update"), Some("""{"symbol":"AAPL","price":150.25,"volume":1000000,"change":2.5}"""), Some("msg-123"), None)
    jsonPath("$.symbol").is("AAPL").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("AAPL"), None)
    jsonPath("$.price").ofType[Double].gt(100.0).check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(150.25), None)
  }
}
