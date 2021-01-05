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

package io.gatling.core.check.jmespath

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.core.check.jsonpath.JsonFilter
import io.gatling.core.json.JsonParsers

class JmesPathExtractorSpec extends BaseSpec with ValidationValues {

  private val jsonParsers = new JsonParsers
  private val jmesPaths = new JmesPaths(Long.MaxValue)

  private val json = """{
                       |  "locations": [
                       |    {"name": "Seattle", "state": "WA"},
                       |    {"name": "New York", "state": "NY"},
                       |    {"name": "Bellevue", "state": "WA"},
                       |    {"name": "Olympia", "state": "WA"}
                       |  ]
                       |}""".stripMargin

  def testSingle[T: JsonFilter](path: String, json: String, expected: Option[T]): Unit = {
    val extractor = new JmesPathExtractor[T]("jmesPath", path, jmesPaths)
    extractor(jsonParsers.parse(json)).succeeded shouldBe expected
  }

  "extract" should "be able to return a single String" in {
    testSingle[String]("locations[0].name", json, Some("Seattle"))
  }

  it should "be able to serialize a JSON object" in {
    testSingle[String]("locations[0]", json, Some("""{"name":"Seattle","state":"WA"}"""))
  }

  it should "be able to return a JSON array as a Seq" in {
    testSingle[Seq[Any]]("locations[?state == 'WA'].name", json, Some(Seq("Seattle", "Bellevue", "Olympia")))
  }

  it should "be able to return a JSON object as a Map" in {
    testSingle[Map[String, Any]](
      "locations[?state == 'WA'].name | sort(@) | {WashingtonCities: join(', ', @)}",
      json,
      Some(Map("WashingtonCities" -> "Bellevue, Olympia, Seattle"))
    )
  }
}
