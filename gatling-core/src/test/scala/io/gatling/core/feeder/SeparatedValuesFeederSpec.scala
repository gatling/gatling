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
package io.gatling.core.feeder

import io.gatling.core.structure.ScenarioContext

import io.gatling.BaseSpec
import io.gatling.core.config.GatlingConfiguration

class SeparatedValuesFeederSpec extends BaseSpec with FeederSupport {

  implicit val configuration = GatlingConfiguration.loadForTest()

  "tsv" should "handle file without escape char" in {
    val data = tsv("sample1.tsv").build(mock[ScenarioContext]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with escape char" in {
    val data = tsv("sample2.tsv").build(mock[ScenarioContext]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "ssv" should "not handle file without escape char" in {
    val data = ssv("sample1.tsv").build(mock[ScenarioContext]).toArray
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with escape char" in {
    val data = ssv("sample2.ssv").build(mock[ScenarioContext]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "csv" should "not handle file without escape char" in {
    val data = csv("sample1.tsv").build(mock[ScenarioContext]).toArray
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with escape char" in {
    val data = csv("sample2.csv").build(mock[ScenarioContext]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "SeparatedValuesParser" should "have a proper raw split" in {
    val data = tsv("sample1.tsv", rawSplit = true).build(mock[ScenarioContext]).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "SeparatedValuesParser" should "throw an exception when provided with bad resource" in {
    import io.gatling.core.feeder.SeparatedValuesParser._
    an[Exception] should be thrownBy
      stream(this.getClass.getClassLoader.getResourceAsStream("empty.csv"), CommaSeparator, '\'', rawSplit = false)
  }
}
