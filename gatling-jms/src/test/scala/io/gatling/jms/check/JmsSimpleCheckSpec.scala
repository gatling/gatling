/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import javax.jms._

import scala.collection.mutable

import io.gatling.BaseSpec
import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.jms._

class JmsSimpleCheckSpec extends BaseSpec with JmsDsl with MockMessage {

  implicit def cache = mutable.Map.empty[Any, Any]

  val session = Session("mockSession", 0)
  val check = simpleCheck {
    case tm: TextMessage => tm.getText == "OK"
    case _               => false
  }

  "simple check" should "return success if condition is true" in {
    check.check(textMessage("OK"), session) shouldBe a[Success[_]]
  }

  it should "return failure if condition is false" in {
    check.check(textMessage("KO"), session) shouldBe a[Failure]
  }

  it should "return failure if message is not TextMessage" in {
    check.check(message, session) shouldBe a[Failure]
  }
}
