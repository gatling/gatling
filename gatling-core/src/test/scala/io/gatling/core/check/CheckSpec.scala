/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
package io.gatling.core.check

import scala.collection.mutable

import io.gatling.BaseSpec
import io.gatling.commons.validation.Failure
import io.gatling.commons.validation.Validation
import io.gatling.core.session.Session

class CheckSpec extends BaseSpec {

  case class SimpleCheck(msg: String) extends Check[String] {
    def check(response: String, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult] = {
      Failure(msg)
    }
  }

  object SimpleSuccess extends Check[String] {
    def check(response: String, session: Session)(implicit cache: mutable.Map[Any, Any]): Validation[CheckResult] = {
      CheckResult.NoopCheckResultSuccess
    }
  }

  "CheckSpec" should "gather messages from all failures" in {
    val (_, failure) = Check.check("test", Session("test", 1),
      List(SimpleCheck("first"), SimpleSuccess, SimpleCheck("second"), SimpleSuccess))
    failure shouldBe a [Some[_]]
    failure should contain (Failure("first\nsecond"))
  }

}
