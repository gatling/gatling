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

package io.gatling.commons.util

import io.gatling.BaseSpec

class ArraysSpec extends BaseSpec {
  "shuffle" should "not introduce duplicate entries" in {
    val array = Iterator.from(0).take(100).toArray
    val original = array.toVector.toString
    Arrays.shuffle(array)
    array.toSet.size shouldBe array.length

    array.toVector.toString should not be original
  }

  it should "not crash on empty array" in {
    Arrays.shuffle(Array.empty[String])
  }
}
