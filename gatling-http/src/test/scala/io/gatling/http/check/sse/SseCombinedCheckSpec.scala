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
import io.gatling.core.check.{ Check, CheckResult }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SseCombinedCheckSpec extends AnyFlatSpecLike with Matchers with ValidationValues with CoreDsl with HttpDsl with EmptySession {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  "Combined field and JSON checks" should "work together on same event" in {
    val event = mockServerSentEvent(Some("price_update"), Some("""{"symbol":"AAPL","price":150.25,"volume":1000000}"""), Some("msg-123"), Some(5000))

    sseEvent.is("price_update").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("price_update"), None)
    sseId.is("msg-123").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("msg-123"), None)
    sseRetry.is(5000).check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(5000), None)

    jsonPath("$.symbol").is("AAPL").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("AAPL"), None)
    jsonPath("$.price").ofType[Double].gt(100.0).check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(150.25), None)
  }

  "Multiple checks on minimal event" should "handle missing fields gracefully" in {
    val event = mockServerSentEvent(None, Some("""{"message":"hello"}"""), None, None)

    sseEvent.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
    sseId.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
    sseRetry.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)

    jsonPath("$.message").is("hello").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("hello"), None)
  }

  "Event with no data field" should "allow field checks but fail data checks" in {
    val event = mockServerSentEvent(Some("heartbeat"), None, Some("hb-001"), None)

    sseEvent.is("heartbeat").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("heartbeat"), None)
    sseId.is("hb-001").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("hb-001"), None)

    sseData.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)

    jsonPath("$.anything").find.exists
      .check(event, emptySession, Check.newPreparedCache)
      .failed shouldBe "jsonPath($.anything).find.exists preparation crashed: No SSE data field"
  }

  "Realistic stock ticker scenario" should "validate complete message" in {
    val event = mockServerSentEvent(
      Some("quote"),
      Some("""{"symbol":"TSLA","bid":245.50,"ask":245.75,"last":245.60,"volume":15234567,"timestamp":1234567890}"""),
      Some("quote-56789"),
      None
    )

    sseEvent.in("quote", "trade", "news").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("quote"), None)

    val cache = Check.newPreparedCache
    jsonPath("$.symbol").is("TSLA").check(event, emptySession, cache).succeeded shouldBe CheckResult(Some("TSLA"), None)
    jsonPath("$.bid").ofType[Double].check(event, emptySession, cache).succeeded shouldBe CheckResult(Some(245.50), None)
    jsonPath("$.ask").ofType[Double].check(event, emptySession, cache).succeeded shouldBe CheckResult(Some(245.75), None)
    jsonPath("$.volume").ofType[Int].gt(1000000).check(event, emptySession, cache).succeeded shouldBe CheckResult(Some(15234567), None)

    sseId.saveAs("lastQuoteId").check(event, emptySession, cache).succeeded shouldBe CheckResult(Some("quote-56789"), Some("lastQuoteId"))
  }

  "Text matching on data" should "work alongside field checks" in {
    val event = mockServerSentEvent(Some("notification"), Some("User Alice logged in from IP 192.168.1.100"), Some("notif-001"), None)

    sseEvent.is("notification").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("notification"), None)
    substring("Alice").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(5), None)
    super[CoreDsl].regex("""IP (\d+\.\d+\.\d+\.\d+)""").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some("192.168.1.100"),
      None
    )
  }

  "Array data with field checks" should "handle complex structures" in {
    val event = mockServerSentEvent(
      Some("batch_update"),
      Some("""[{"id":1,"status":"completed"},{"id":2,"status":"pending"},{"id":3,"status":"completed"}]"""),
      Some("batch-42"),
      None
    )

    sseEvent.is("batch_update").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("batch_update"), None)
    jsonPath("$[*].status").findAll.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Seq("completed", "pending", "completed")),
      None
    )
    jsonPath("$[?(@.status == 'completed')].id").ofType[Int].findAll.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Seq(1, 3)),
      None
    )
  }
}
