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

package io.gatling.jms.check

import java.util.{ HashMap => JHashMap }
import javax.jms.Message

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.commons.validation.Success
import io.gatling.core.CoreDsl
import io.gatling.core.EmptySession
import io.gatling.core.check.CheckResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.jms.{ JmsCheck, MockMessage }

import org.scalatest.prop.TableDrivenPropertyChecks

class JmsConditionalCheckSpec
    extends BaseSpec
    with ValidationValues
    with MockMessage
    with CoreDsl
    with JmsCheckSupport
    with TableDrivenPropertyChecks
    with EmptySession {
  override implicit def configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private def jmap[K, V] = new JHashMap[K, V]

  private val testResponses = Table(
    ("msg", "msgType"),
    (textMessage("""[{"id":"1072920417"},"id":"1072920418"]"""), "TextMessage"),
    (bytesMessage("""[{"id":"1072920417"},"id":"1072920418"]""".getBytes(configuration.core.charset)), "BytesMessage")
  )

  forAll(testResponses) { (response: Message, msgType: String) =>
    s"checkIf.true.succeed for $msgType" should "perform the succeed nested check" in {
      val thenCheck: JmsCheck = substring(""""id":"""").count
      val check: JmsCheck = checkIf((_: Message, _: Session) => Success(true))(thenCheck)
      check.check(response, emptySession, jmap[Any, Any]).succeeded shouldBe CheckResult(Some(2), None)
    }

    s"checkIf.true.failed for $msgType" should "perform the failed nested check" in {
      val substringValue = """"foo":""""
      val thenCheck: JmsCheck = substring(substringValue).findAll.exists
      val check: JmsCheck = checkIf((_: Message, _: Session) => Success(true))(thenCheck)
      check.check(response, emptySession, jmap[Any, Any]).failed shouldBe s"substring($substringValue).findAll.exists, found nothing"
    }

    s"checkIf.false.succeed for $msgType" should "not perform the succeed nested check" in {
      val thenCheck: JmsCheck = substring(""""id":"""").count
      val check: JmsCheck = checkIf((_: Message, _: Session) => Success(false))(thenCheck)
      check.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(None, None)
    }

    s"checkIf.false.failed for $msgType" should "not perform the failed nested check" in {
      val substringValue = """"foo":""""
      val thenCheck: JmsCheck = substring(substringValue).findAll.exists
      val check: JmsCheck = checkIf((_: Message, _: Session) => Success(false))(thenCheck)
      check.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(None, None)
    }
  }
}
