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
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.action.sse.fsm.ServerSentEvent

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SseRegexCheckSpec extends AnyFlatSpecLike with Matchers with ValidationValues with CoreDsl with HttpDsl with EmptySession {
  object RegexSupport extends SseCheckSupport

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val materializer: CheckMaterializer[RegexCheckType, SseCheck, ServerSentEvent, String] =
    SseCheckMaterializer.Regex

  private val regexCheck = super[CoreDsl].regex(_)

  "regex.find.exists" should "extract from data field" in {
    val event = mockServerSentEvent(None, Some("""{"symbol":"AAPL","price":150}"""), None, None)
    regexCheck(""""symbol":"(.+?)"""").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("AAPL"), None)
  }

  it should "find first occurrence in data" in {
    val event = mockServerSentEvent(None, Some("""[{"id":"1072920417"},"id":"1072920418"]"""), None, None)
    regexCheck(""""id":"(.+?)"""").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("1072920417"), None)
  }

  it should "return empty string when data is empty" in {
    val event = mockServerSentEvent(None, Some(""), None, None)
    regexCheck(""".*""").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(""), None)
  }

  it should "use empty string when data field is absent" in {
    val event = mockServerSentEvent(Some("heartbeat"), None, None, None)
    regexCheck(""".*""").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(""), None)
  }

  it should "fail when pattern doesn't match" in {
    val event = mockServerSentEvent(None, Some("no match here"), None, None)
    val pattern = """"id":"(.+?)""""
    regexCheck(pattern).find.exists.check(event, emptySession, Check.newPreparedCache).failed shouldBe s"regex($pattern).find.exists, found nothing"
  }

  "regex.findAll.exists" should "find all occurrences in data field" in {
    val event = mockServerSentEvent(None, Some("""[{"id":"1072920417"},{"id":"1072920418"}]"""), None, None)
    regexCheck(""""id":"(.+?)"""").findAll.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Seq("1072920417", "1072920418")),
      None
    )
  }

  it should "fail when finding nothing" in {
    val event = mockServerSentEvent(None, Some("no matches"), None, None)
    val pattern = """"foo":"(.+?)""""
    regexCheck(pattern).findAll.exists.check(event, emptySession, Check.newPreparedCache).failed shouldBe s"regex($pattern).findAll.exists, found nothing"
  }

  "regex.count.exists" should "count occurrences in data field" in {
    val event = mockServerSentEvent(None, Some("""[{"id":"1"},{"id":"2"},{"id":"3"}]"""), None, None)
    regexCheck(""""id":"(.+?)"""").count.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(3), None)
  }

  it should "return 0 when finding nothing" in {
    val event = mockServerSentEvent(None, Some("no matches"), None, None)
    regexCheck(""""foo":"(.+?)"""").count.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(0), None)
  }

  "regex with complex SSE message" should "only search in data field, not other fields" in {
    val event = mockServerSentEvent(Some("price_update"), Some("""{"symbol":"AAPL"}"""), Some("msg-123"), None)
    // matches only in data field, not in event or id fields
    regexCheck(""""symbol":"(.+?)"""").find.exists.check(event, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("AAPL"), None)
  }

  it should "not find event field content" in {
    val event = mockServerSentEvent(Some("price_update"), Some("""{"value":100}"""), None, None)
    // "price_update" exists in event field but not in data
    regexCheck("""price_update""").find.exists
      .check(event, emptySession, Check.newPreparedCache)
      .failed shouldBe "regex(price_update).find.exists, found nothing"
  }
}
