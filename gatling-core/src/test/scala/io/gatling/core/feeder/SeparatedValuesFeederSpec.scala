/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.feeder.SeparatedValuesParser._

class SeparatedValuesFeederSpec extends BaseSpec with FeederSupport {

  private implicit val configuration = GatlingConfiguration.loadForTest()

  "csv" should "not handle file without quote char" in {
    val data = csv("sample1.tsv").readRecords
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with quote char" in {
    val data = csv("sample2.csv").readRecords
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "be compliant with the RFC4180 by default and use \" as escape char" in {
    val data = csv("sample4.csv").readRecords
    data shouldBe Array(Map("id" -> "id", "payload" -> """{"key1": "value1", "key2": "value3"}"""))
  }

  "tsv" should "handle file without quote char" in {
    val data = tsv("sample1.tsv").readRecords
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with quote char" in {
    val data = tsv("sample2.tsv").readRecords
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "ssv" should "not handle file without quote char" in {
    val data = ssv("sample1.ssv").readRecords
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with quote char" in {
    val data = ssv("sample2.ssv").readRecords
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "SeparatedValuesParser.stream" should "throw an exception when provided with bad resource" in {
    an[Exception] should be thrownBy
      stream(CommaSeparator, quoteChar = '\'', configuration.core.charset)(getClass.getClassLoader.getResourceAsStream("empty.csv"))
  }
}
