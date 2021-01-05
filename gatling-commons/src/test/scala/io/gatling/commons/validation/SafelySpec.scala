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

package io.gatling.commons.validation

import scala.util.control.NoStackTrace

import io.gatling.BaseSpec

class SafelySpec extends BaseSpec {

  "safely" should "returned the provided Validation if it didn't throw exceptions" in {
    safely()(1.success) shouldBe 1.success
  }

  it should "return a failure if the provided Validation threw exceptions" in {
    def exceptionThrower = {
      def thrower = throw new Exception("Woops") with NoStackTrace

      thrower
      Success(1)
    }

    safely()(exceptionThrower) shouldBe "j.l.Exception: Woops".failure
    safely(_ + "y")(exceptionThrower) shouldBe "j.l.Exception: Woopsy".failure
  }
}
