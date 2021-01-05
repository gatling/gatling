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

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.commons.validation._
import io.gatling.core.CoreDsl
import io.gatling.core.EmptySession
import io.gatling.core.config.GatlingConfiguration
import io.gatling.jms.{ JmsCheck, MockMessage }

class JmsXPathCheckSpec extends BaseSpec with ValidationValues with MockMessage with CoreDsl with JmsCheckSupport with EmptySession {

  override val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private val check: JmsCheck = xpath("/ok").find

  "xpath check" should "return success if condition is true" in {
    check.check(textMessage("<ok></ok>"), emptySession, new JHashMap[Any, Any]) shouldBe a[Success[_]]
  }

  it should "return failure if condition is false" in {
    check.check(textMessage("<ko></ko>"), emptySession, new JHashMap[Any, Any]) shouldBe a[Failure]
  }

  it should "return failure if message is not TextMessage" in {
    check.check(message, emptySession, new JHashMap[Any, Any]).failed.message should include("Unsupported message type")
  }
}
