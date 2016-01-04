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
package io.gatling.core.controller.inject

import scala.concurrent.duration._

import io.gatling.BaseSpec

class InjectionProfileSpec extends BaseSpec {

  "Inserting a pause between steps" should "produce the sum of the other steps" in {

    val steps = Seq(AtOnceInjection(1), NothingForInjection(2 seconds), AtOnceInjection(1))
    val profile = InjectionProfile(steps)
    profile.userCount shouldBe 2
    profile.allUsers.toSeq shouldBe Seq(0 seconds, 2 seconds)
  }
}
