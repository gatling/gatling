/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class JsonFeederSpec extends AnyFlatSpecLike with Matchers with FeederSupport {
  private implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val jsonParsers: JsonParsers = new JsonParsers

  "jsonFile#readRecords" should "handle proper JSON file" in {
    val data = jsonFile("test.json").readRecords

    data.length shouldBe 2
    val head = data.head
    head("id") shouldBe 19434
    head("company") shouldBe Map("id" -> 18971)
  }

  "jsonFile#recordsCount" should "handle proper JSON file" in {
    val count = jsonFile("test.json").recordsCount

    count shouldBe 2
  }

  "jsonUrl#readRecords" should "retrieve and handle proper JSON file" in {
    val data = jsonUrl(getClass.getClassLoader.getResource("test.json").toString).readRecords
    data.length shouldBe 2
    data.head("id") shouldBe 19434
  }

  "jsonUrl#recordsCount" should "retrieve and handle proper JSON file" in {
    val count = jsonUrl(getClass.getClassLoader.getResource("test.json").toString).recordsCount

    count shouldBe 2
  }
}
