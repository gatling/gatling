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

import java.io.{ ByteArrayInputStream, InputStream }
import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.BaseSpec

class BatchedSeparatedValuesFeederSpec extends BaseSpec {

  private val streamer = SeparatedValuesParser.stream(',', '"', UTF_8)

  private val csvContent =
    s"""column1,column2
         |line1_1,line1_2
         |line2_1,line2_2
         |line3_1,line3_2
         |line4_1,line4_2
         |line5_1,line5_2
         |""".stripMargin

  private def newInputStream(text: String): InputStream =
    new ByteArrayInputStream(text.getBytes(UTF_8))

  "QueueBatchedSeparatedValuesFeeder" should "feed full content" in {
    new QueueBatchedSeparatedValuesFeeder(newInputStream(csvContent), streamer).toVector shouldBe Vector(
      Map("column1" -> "line1_1", "column2" -> "line1_2"),
      Map("column1" -> "line2_1", "column2" -> "line2_2"),
      Map("column1" -> "line3_1", "column2" -> "line3_2"),
      Map("column1" -> "line4_1", "column2" -> "line4_2"),
      Map("column1" -> "line5_1", "column2" -> "line5_2")
    )
  }

  it should "throw a ArrayIndexOutOfBoundsException on empty content" in {
    a[ArrayIndexOutOfBoundsException] should be thrownBy new QueueBatchedSeparatedValuesFeeder(newInputStream(""), streamer)
  }

  it should "return an empty feeder when there's no record" in {
    new QueueBatchedSeparatedValuesFeeder(newInputStream(s"""column1,column2
                                                            |""".stripMargin), streamer).hasNext shouldBe false
  }

  "RandomBatchedSeparatedValuesFeeder" should "feed an infinite stream of different records" in {
    val takeSize = 100
    val records = new RandomBatchedSeparatedValuesFeeder(newInputStream(csvContent), streamer, 3).take(takeSize).toVector
    records.size shouldBe takeSize
    records.toSet.size shouldBe 5
  }

  "ShuffleBatchedSeparatedValuesFeeder" should "feed a finite stream of different records" in {
    val takeSize = 5
    val feeder = new ShuffleBatchedSeparatedValuesFeeder(newInputStream(csvContent), streamer, 3)
    val records = feeder.take(takeSize).toVector
    records.size shouldBe takeSize
    records.toSet.size shouldBe 5
    feeder.hasNext shouldBe false
  }

  "CircularBatchedSeparatedValuesFeeder" should "feed a finite stream of expected records" in {
    new CircularBatchedSeparatedValuesFeeder(newInputStream(csvContent), streamer).take(10).toVector shouldBe Vector(
      Map("column1" -> "line1_1", "column2" -> "line1_2"),
      Map("column1" -> "line2_1", "column2" -> "line2_2"),
      Map("column1" -> "line3_1", "column2" -> "line3_2"),
      Map("column1" -> "line4_1", "column2" -> "line4_2"),
      Map("column1" -> "line5_1", "column2" -> "line5_2"),
      Map("column1" -> "line1_1", "column2" -> "line1_2"),
      Map("column1" -> "line2_1", "column2" -> "line2_2"),
      Map("column1" -> "line3_1", "column2" -> "line3_2"),
      Map("column1" -> "line4_1", "column2" -> "line4_2"),
      Map("column1" -> "line5_1", "column2" -> "line5_2")
    )
  }
}
