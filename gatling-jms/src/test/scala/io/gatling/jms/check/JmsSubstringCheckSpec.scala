/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.jms.check

import java.util.{ HashMap => JHashMap }

import io.gatling.core.CoreDsl
import io.gatling.core.check.CheckResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.jms.MockMessage
import io.gatling.{ BaseSpec, ValidationValues }
import javax.jms.Message
import org.scalatest.prop.TableDrivenPropertyChecks

class JmsSubstringCheckSpec extends BaseSpec with ValidationValues with MockMessage with CoreDsl with JmsCheckSupport with TableDrivenPropertyChecks {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private def jmap[K, V] = new JHashMap[K, V]

  private val session = Session("mockSession", 0, System.currentTimeMillis())

  private val testResponses = Table(
    ("msg", "msgType"),
    (textMessage("""[{"id":"1072920417"},"id":"1072920418"]"""), "TextMessage"),
    (bytesMessage("""[{"id":"1072920417"},"id":"1072920418"]""".getBytes()), "BytesMessage")
  )

  "substring.find.exists for TextMessage" should "find single result" in {
    val response = textMessage("""{"id":"1072920417"}""")
    substring(""""id":"""").find.exists.check(response, session, jmap[Any, Any]).succeeded shouldBe CheckResult(Some(1), None)
  }

  "substring.find.exists for BytesMessage" should "find single result" in {
    val response = bytesMessage("""{"id":"1072920417"}""".getBytes())
    substring(""""id":"""").find.exists.check(response, session, jmap[Any, Any]).succeeded shouldBe CheckResult(Some(1), None)
  }

  forAll(testResponses) { (response: Message, msgType: String) =>
    s"substring.find.exists for $msgType" should "find first occurrence" in {
      substring(""""id":"""").find.exists.check(response, session, jmap[Any, Any]).succeeded shouldBe CheckResult(Some(2), None)
    }

    s"substring.findAll.exists for $msgType" should "find all occurrences" in {
      substring(""""id":"""").findAll.exists.check(response, session, jmap[Any, Any]).succeeded shouldBe CheckResult(Some(Seq(2, 21)), None)
    }

    it should "fail when finding nothing instead of returning an empty Seq" in {
      val substringValue = """"foo":""""
      substring(substringValue).findAll.exists
        .check(response, session, jmap[Any, Any])
        .failed shouldBe s"substring($substringValue).findAll.exists, found nothing"
    }

    s"substring.count.exists for $msgType" should "find all occurrences" in {
      substring(""""id":"""").count.exists.check(response, session, jmap[Any, Any]).succeeded shouldBe CheckResult(Some(2), None)
    }

    it should "return 0 when finding nothing instead of failing" in {
      substring(""""foo":"""").count.exists.check(response, session, jmap[Any, Any]).succeeded shouldBe CheckResult(Some(0), None)
    }
  }
}
