/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import io.gatling.BaseSpec
import io.gatling.commons.util.Maps._

class MapsSpec extends BaseSpec {

  "longMerger copy" should "return the long value that is copied" in {
    val number, result = 10L
    val computed = LongMerger.copy(number)
    computed shouldBe result
  }

  "longMerger merge" should "add the long value to the one we merged" in {
    val first = 8L
    val second = 9L
    val result = first + second
    val computed = LongMerger.merge(first, second)
    computed shouldBe result
  }

  "seqMerger copy" should "return the sequence that is copied" in {
    val sequence, result = Seq(1, 2, 3, 4, 5)
    val computed = seqMerger[Int].copy(sequence)
    computed shouldBe result
  }

  "seqMerger merge" should "concatenate the sequence with the one we merge" in {
    val sequence1 = Seq(1, 2, 3, 4, 5, 6)
    val sequence2 = Seq(1, 8, 9)
    val result = Seq(1, 2, 3, 4, 5, 6, 1, 8, 9)
    val computed = seqMerger[Int].merge(sequence1, sequence2)
    computed shouldBe result
  }

  "mapMerge merge" should "concatenate map when there is no duplicate key with long values" in {
    val mapTest1 = Map(Seq(12, 12) -> 1L, Seq(12, 1) -> 2L, Seq(12, 3) -> 3L)
    val mapTest2 = Map(Seq(3, 9) -> 4L, Seq(23) -> 5L)
    val mapResult = Map(Seq(12, 12) -> 1L, Seq(12, 1) -> 2L, Seq(12, 3) -> 3L, Seq(3, 9) -> 4L, Seq(23) -> 5L)
    val mapComputed = mapMerger[Seq[Int], Long].merge(mapTest1, mapTest2)
    mapComputed shouldBe mapResult
  }

  it should "concatenate map when there is no duplicate key with Seq values" in {
    val mapTest1 = Map(Seq("one", "two") -> Seq(1, 2), Seq("three", "four") -> Seq(8))
    val mapTest2 = Map(Seq("five") -> Seq(3))
    val mapResult = Map(Seq("one", "two") -> Seq(1, 2), Seq("three", "four") -> Seq(8), Seq("five") -> Seq(3))
    val mapComputed = mapTest1.mergeWith(mapTest2)
    mapComputed shouldBe mapResult
  }

  it should "add long values when there are duplicate keys" in {
    val mapTest1 = Map("one" -> 10L, "two" -> 2L, "three" -> 6L)
    val mapTest2 = Map("one" -> 4L, "two" -> 8L, "four" -> 90L)
    val mapResult = Map("one" -> 14L, "two" -> 10L, "three" -> 6L, "four" -> 90L)
    val mapComputed = mapTest1.mergeWith(mapTest2)
    mapComputed shouldBe mapResult
  }

  it should "concatenate sequence values when there are duplicate keys" in {
    val mapTest1 = Map("one" -> Seq(1, 2, 3), "two" -> Seq(8), "three" -> Seq(4, 7))
    val mapTest2 = Map("one" -> Seq(1, 2, 3), "two" -> Seq(10, 11), "four" -> Seq(8))
    val mapResult = Map("one" -> Seq(1, 2, 3, 1, 2, 3), "two" -> Seq(8, 10, 11), "three" -> Seq(4, 7), "four" -> Seq(8))
    val mapComputed = mapTest1.mergeWith(mapTest2)
    mapComputed shouldBe mapResult
  }

  "mergeWith" should "concatenate map when there is no duplicate key with long values" in {
    val mapTest1 = Map(Seq(1, 2) -> 1L, Seq(3, 4) -> 2L, Seq(5, 6) -> 3L)
    val mapTest2 = Map(Seq(7, 8) -> 4L, Seq(10) -> 5L)
    val mapResult = Map(Seq(1, 2) -> 1L, Seq(3, 4) -> 2L, Seq(5, 6) -> 3L, Seq(7, 8) -> 4L, Seq(10) -> 5L)
    val mapComputed = mapTest1.mergeWith(mapTest2)
    mapComputed shouldBe mapResult
  }

  it should "concatenate map when there is no duplicate key with Seq values" in {
    val mapTest1 = Map(Seq("one", "two") -> Seq(1, 2), Seq("three", "four") -> Seq(8))
    val mapTest2 = Map(Seq("five") -> Seq(3))
    val mapResult = Map(Seq("one", "two") -> Seq(1, 2), Seq("three", "four") -> Seq(8), Seq("five") -> Seq(3))
    val mapComputed = mapTest1.mergeWith(mapTest2)
    mapComputed shouldBe mapResult
  }

  it should "add long values when there are duplicate keys" in {
    val mapTest1 = Map("one" -> 1L, "two" -> 2L, "three" -> 3L)
    val mapTest2 = Map("one" -> 4L, "two" -> 5L, "four" -> 8L)
    val mapResult = Map("one" -> 5L, "two" -> 7L, "three" -> 3L, "four" -> 8L)
    val mapComputed = mapTest1.mergeWith(mapTest2)
    mapComputed shouldBe mapResult
  }

  it should "concatenate sequence values when there are duplicate keys" in {
    val mapTest1 = Map("one" -> Seq(1, 2, 3), "two" -> Seq(8), "three" -> Seq(4, 7))
    val mapTest2 = Map("one" -> Seq(1, 2, 3), "two" -> Seq(10, 11), "four" -> Seq(8))
    val mapResult = Map("one" -> Seq(1, 2, 3, 1, 2, 3), "two" -> Seq(8, 10, 11), "three" -> Seq(4, 7), "four" -> Seq(8))
    val mapComputed = mapTest1.mergeWith(mapTest2)
    mapComputed shouldBe mapResult
  }

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
