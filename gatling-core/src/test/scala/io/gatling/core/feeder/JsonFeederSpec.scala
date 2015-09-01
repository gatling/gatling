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

import io.gatling.BaseSpec
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers

class JsonFeederSpec extends BaseSpec with FeederSupport {

  implicit val configuration = GatlingConfiguration.loadForTest()
  implicit val jsonParsers = JsonParsers()

  "jsonFile" should "handle proper JSON file" in {
    val data = jsonFile("test.json").build(mock[ScenarioContext]).toArray

    data.size shouldBe 2
    data(0)("id") shouldBe 19434
  }

  "jsonUrl" should "retrieve and handle proper JSON file" in {
    val data = jsonUrl(getClass.getClassLoader.getResource("test.json").toString).build(mock[ScenarioContext]).toArray
    data.size shouldBe 2
    data(0)("id") shouldBe 19434
  }

  "JsonFeederFileParser" should "throw an exception when provided with bad resource" in {
    an[IllegalArgumentException] should be thrownBy
      new JsonFeederFileParser().stream(this.getClass.getClassLoader.getResourceAsStream("empty.json"))
  }
}
