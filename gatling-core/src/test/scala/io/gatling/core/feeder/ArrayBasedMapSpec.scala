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

package io.gatling.core.feeder

import io.gatling.BaseSpec

class ArrayBasedMapSpec extends BaseSpec {

  "ArrayBasedMap" should "generate iterator with proper records" in {
    ArrayBasedMap(Array("col1", "col2"), Array("val1", "val2")).iterator.toArray shouldBe Array("col1" -> "val1", "col2" -> "val2")
  }

  it should "ignore values in excess" in {
    ArrayBasedMap(Array("col1", "col2"), Array("val1", "val2", "val3")).iterator.toArray shouldBe Array("col1" -> "val1", "col2" -> "val2")
  }

  it should "discard missing trailing values" in {
    ArrayBasedMap(Array("col1", "col2"), Array("val1")).iterator.toArray shouldBe Array("col1" -> "val1")
  }

  it should "return an empty iterator when there's no value" in {
    ArrayBasedMap(Array("col1", "col2"), Array.empty).iterator shouldBe empty
  }
}
