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

package io.gatling.http.util

import scala.jdk.CollectionConverters._

import io.gatling.BaseSpec

class ListsSpec extends BaseSpec {

  "isSameSetAssumingNoDuplicate" should "return false when lists don't have the same size" in {
    Lists.isSameSetAssumingNoDuplicate(List(1, 2).asJava, List(1).asJava) shouldBe false
  }

  it should "return true when lists are identical" in {
    Lists.isSameSetAssumingNoDuplicate(List(1, 2).asJava, List(1, 2).asJava) shouldBe true
  }

  it should "return true when lists are identical but in a different order" in {
    Lists.isSameSetAssumingNoDuplicate(List(1, 2).asJava, List(2, 1).asJava) shouldBe true
  }

  it should "return false when lists are not identical" in {
    Lists.isSameSetAssumingNoDuplicate(List(1, 2).asJava, List(3, 1).asJava) shouldBe false
  }
}
