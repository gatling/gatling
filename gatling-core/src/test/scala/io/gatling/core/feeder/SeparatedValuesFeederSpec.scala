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
package io.gatling.core.feeder

import io.gatling.BaseSpec
import io.gatling.core.CoreComponents
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.config.GatlingConfiguration

import org.mockito.Mockito._

class SeparatedValuesFeederSpec extends BaseSpec with FeederSupport {

  implicit val configuration = GatlingConfiguration.loadForTest()

  def scenarioContext(cfg: GatlingConfiguration = configuration) = {
    val ctx = mock[ScenarioContext]
    val coreComponents = mock[CoreComponents]
    when(coreComponents.configuration) thenReturn cfg
    when(ctx.coreComponents) thenReturn coreComponents
    ctx
  }

  "csv" should "not handle file without quote char" in {
    val data = csv("sample1.tsv").build(scenarioContext()).toArray
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with quote char" in {
    val data = csv("sample2.csv").build(scenarioContext()).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "allow an escape char" in {
    val data = csv("sample3.csv", escapeChar = '\\').build(scenarioContext()).toArray
    data shouldBe Array(Map("id" -> "id", "payload" -> """{"k1": "v1", "k2": "v2"}"""))
  }

  it should "be compliant with the RFC4180 by default (no escape char by default)" in {
    val data = csv("sample4.csv").build(scenarioContext()).toArray
    data shouldBe Array(Map("id" -> "id", "payload" -> """{"key": "\"value\""}"""))
  }

  "tsv" should "handle file without quote char" in {
    val data = tsv("sample1.tsv").build(scenarioContext()).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with quote char" in {
    val data = tsv("sample2.tsv").build(scenarioContext()).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "ssv" should "not handle file without quote char" in {
    val data = ssv("sample1.ssv").build(scenarioContext()).toArray
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with quote char" in {
    val data = ssv("sample2.ssv").build(scenarioContext()).toArray
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "SeparatedValuesParser.stream" should "throw an exception when provided with bad resource" in {
    import io.gatling.core.feeder.SeparatedValuesParser._
    an[Exception] should be thrownBy
      stream(this.getClass.getClassLoader.getResourceAsStream("empty.csv"), CommaSeparator, quoteChar = '\'', escapeChar = 0)
  }
}
