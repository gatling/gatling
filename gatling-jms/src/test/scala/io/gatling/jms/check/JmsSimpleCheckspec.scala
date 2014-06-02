/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.jms.check

import org.specs2.mutable.Specification
import io.gatling.jms.Predef._
import javax.jms._
import io.gatling.core.session.Session
import io.gatling.core.validation.{ Success, Failure }
import io.gatling.jms.MockMessage
import scala.collection.mutable

class JmsSimpleCheckSpec extends Specification with MockMessage {

  implicit def cache = mutable.Map.empty[Any, Any]

  "simple check" should {
    val session = Session("mockSession", "mockUserName")
    val check = simpleCheck {
      case tm: TextMessage if tm.getText() == "OK" => true
      case tm: TextMessage if tm.getText() == "KO" => false
      case _                                       => false
    }

    "return success if condition is true" in {
      check.check(textMessage("OK"), session) must beAnInstanceOf[Success[_]]
    }

    "return failure if condition is false" in {
      check.check(textMessage("KO"), session) must beAnInstanceOf[Failure]
    }

    "return failure if message is not TextMessage" in {
      check.check(message, session) must beAnInstanceOf[Failure]
    }
  }
}
