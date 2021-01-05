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

package io.gatling.core.feeder

import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.BaseSpec
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers

class JsonFeederSpec extends BaseSpec with FeederSupport {

  private implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val jsonParsers: JsonParsers = new JsonParsers

  "jsonFile" should "handle proper JSON file" in {
    val data = jsonFile("test.json").readRecords

    data.length shouldBe 2
    data.head("id") shouldBe 19434
  }

  "jsonUrl" should "retrieve and handle proper JSON file" in {
    val data = jsonUrl(getClass.getClassLoader.getResource("test.json").toString).readRecords
    data.length shouldBe 2
    data.head("id") shouldBe 19434
  }

  "JsonFeederFileParser" should "throw an exception when provided with bad resource" in {
    an[IllegalArgumentException] should be thrownBy
      new JsonFeederFileParser(jsonParsers).stream(this.getClass.getClassLoader.getResourceAsStream("empty.json"), UTF_8)
  }
}
