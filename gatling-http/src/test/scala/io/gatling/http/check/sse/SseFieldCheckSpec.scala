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
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.action.sse.fsm.ServerSentEvent

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SseFieldCheckSpec extends AnyFlatSpecLike with Matchers with ValidationValues with CoreDsl with HttpDsl with EmptySession {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  // sseEvent tests
  implicit val sseEventMaterializer: CheckMaterializer[SseEventCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    SseCheckMaterializer.Event

  "sseEvent.find.exists" should "extract event field when present" in {
    val event = mockServerSentEvent(Some("price_update"), None, None, None)
    sseEvent.find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("price_update"), None)
  }

  it should "succeed with optional when event field is absent" in {
    val event = mockServerSentEvent(None, Some("test"), None, None)
    sseEvent.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
  }

  "sseEvent.find.is" should "succeed when event matches" in {
    val event = mockServerSentEvent(Some("snapshot"), None, None, None)
    sseEvent.is("snapshot").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("snapshot"), None)
  }

  it should "fail when event does not match" in {
    val event = mockServerSentEvent(Some("update"), None, None, None)
    sseEvent.is("snapshot").check(event, emptySession, Check.newPreparedCache).failed shouldBe "sseEvent.find.is(snapshot), found update"
  }

  "sseEvent.find.in" should "succeed when event is in list" in {
    val event = mockServerSentEvent(Some("update"), None, None, None)
    sseEvent.in("snapshot", "update", "delete").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("update"), None)
  }

  it should "fail when event is not in list" in {
    val event = mockServerSentEvent(Some("other"), None, None, None)
    sseEvent.in("snapshot", "update").check(event, emptySession, Check.newPreparedCache).failed shouldBe "sseEvent.find.in(snapshot,update), found other"
  }

  // sseData tests
  implicit val sseDataMaterializer: CheckMaterializer[SseDataCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    SseCheckMaterializer.Data

  "sseData.find.exists" should "extract data field when present" in {
    val event = mockServerSentEvent(None, Some("""{"price": 100}"""), None, None)
    sseData.find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("""{"price": 100}"""), None)
  }

  it should "succeed with optional when data field is absent" in {
    val event = mockServerSentEvent(Some("heartbeat"), None, None, None)
    sseData.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
  }

  "sseData.find.notNull" should "succeed when data is present" in {
    val event = mockServerSentEvent(None, Some("test data"), None, None)
    sseData.notNull.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("test data"), None)
  }

  it should "fail when data is absent" in {
    val event = mockServerSentEvent(Some("ping"), None, None, None)
    sseData.notNull.check(event, emptySession, Check.newPreparedCache).failed shouldBe "sseData.find.notNull, found nothing"
  }

  "sseData.saveAs" should "save data to session" in {
    val event = mockServerSentEvent(None, Some("important data"), None, None)
    sseData.saveAs("myData").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("important data"), Some("myData"))
  }

  // sseId tests
  implicit val sseIdMaterializer: CheckMaterializer[SseIdCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    SseCheckMaterializer.Id

  "sseId.find.exists" should "extract id field when present" in {
    val event = mockServerSentEvent(None, None, Some("12345"), None)
    sseId.find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("12345"), None)
  }

  it should "succeed with optional when id field is absent" in {
    val event = mockServerSentEvent(None, Some("test"), None, None)
    sseId.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
  }

  "sseId.is" should "succeed when id matches" in {
    val event = mockServerSentEvent(None, None, Some("abc123"), None)
    sseId.is("abc123").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("abc123"), None)
  }

  it should "fail when id does not match" in {
    val event = mockServerSentEvent(None, None, Some("xyz"), None)
    sseId.is("abc").check(event, emptySession, Check.newPreparedCache).failed shouldBe "sseId.find.is(abc), found xyz"
  }

  "sseId.saveAs" should "save id to session" in {
    val event = mockServerSentEvent(None, None, Some("event-456"), None)
    sseId.saveAs("lastEventId").check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("event-456"), Some("lastEventId"))
  }

  // sseRetry tests
  implicit val sseRetryMaterializer: CheckMaterializer[SseRetryCheckType, SseCheck, ServerSentEvent, ServerSentEvent] =
    SseCheckMaterializer.Retry

  "sseRetry.find.exists" should "extract retry field when present" in {
    val event = mockServerSentEvent(None, None, None, Some(5000))
    sseRetry.find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(5000), None)
  }

  it should "succeed with optional when retry field is absent" in {
    val event = mockServerSentEvent(None, Some("test"), None, None)
    sseRetry.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
  }

  "sseRetry.is" should "succeed when retry matches" in {
    val event = mockServerSentEvent(None, None, None, Some(3000))
    sseRetry.is(3000).check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(3000), None)
  }

  it should "fail when retry does not match" in {
    val event = mockServerSentEvent(None, None, None, Some(5000))
    sseRetry.is(3000).check(event, emptySession, Check.newPreparedCache).failed shouldBe "sseRetry.find.is(3000), found 5000"
  }

  "sseRetry.optional" should "succeed when retry is absent" in {
    val event = mockServerSentEvent(None, Some("test"), None, None)
    sseRetry.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
  }

  it should "succeed and extract when retry is present" in {
    val event = mockServerSentEvent(None, None, None, Some(1000))
    sseRetry.optional.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(1000), None)
  }

  "sseRetry.gt" should "succeed when retry is greater than threshold" in {
    val event = mockServerSentEvent(None, None, None, Some(10000))
    sseRetry.gt(5000).check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(10000), None)
  }

  it should "fail when retry is less than or equal to threshold" in {
    val event = mockServerSentEvent(None, None, None, Some(3000))
    sseRetry.gt(5000).check(event, emptySession, Check.newPreparedCache).failed shouldBe "sseRetry.find.greaterThan(5000), 3000 is not greater than 5000"
  }
}
