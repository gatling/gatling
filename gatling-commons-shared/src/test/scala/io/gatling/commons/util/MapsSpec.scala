/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import scala.collection.mutable.ArrayBuffer

import io.gatling.commons.util.Maps._

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class MapsSpec extends AnyFlatSpecLike with Matchers {

  "forceMapValues" should "apply a function to each value of the map" in {
    val mapTest1 = Map(Seq(1, 2) -> 8, Seq(1) -> 9, Seq(8) -> 10)
    val mapResult = Map(Seq(1, 2) -> 9, Seq(1) -> 10, Seq(8) -> 11)
    val mapComputed = mapTest1.forceMapValues(_ + 1)
    mapComputed shouldBe mapResult
  }

  "groupByKey" should "regroup values in a IterableView object by adding them by key" in {
    val mapTest = Seq(1 -> 2, 2 -> 3, 1 -> 4, 2 -> 6, 3 -> 4, 4 -> 12, 12 -> 4)
    val mapResult = Map(1 -> ArrayBuffer(2, 4), 2 -> ArrayBuffer(3, 6), 3 -> ArrayBuffer(4), 4 -> ArrayBuffer(12), 12 -> ArrayBuffer(4))
    val mapComputed = mapTest.groupByKey[Int](k => k)
    mapComputed shouldBe mapResult
  }

  it should "regroup values in a TraversableOnce object by adding them by key after applying a function on the keys" in {
    val mapTest = Seq(1 -> 2, 2 -> 3, 1 -> 4, 2 -> 6, 3 -> 4, 4 -> 12, 12 -> 4)
    val mapResult = Map(2 -> ArrayBuffer(2, 4), 4 -> ArrayBuffer(3, 6), 6 -> ArrayBuffer(4), 8 -> ArrayBuffer(12), 24 -> ArrayBuffer(4))
    val mapComputed = mapTest.groupByKey[Int](_ * 2)
    mapComputed shouldBe mapResult
  }
}
