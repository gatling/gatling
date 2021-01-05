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

package io.gatling.http.check.body

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.core.CoreDsl
import io.gatling.core.EmptySession
import io.gatling.core.check.{ Check, CheckMaterializer, CheckResult }
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response

import com.fasterxml.jackson.databind.JsonNode

class HttpBodyJsonpJsonPathCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl with EmptySession {

  implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  implicit val materializer: CheckMaterializer[JsonPathCheckType, HttpCheck, Response, JsonNode] =
    HttpBodyJsonPathCheckMaterializer.instance(new JsonParsers)

  private val storeJson = """someJsMethod({ "store": {
                            |    "book": "In store"
                            |  },
                            |  "street": {
                            |    "book": "On the street"
                            |  }
                            |});""".stripMargin.replaceAll("""[\r\n]""", "")

  "jsonpJsonPath.find.exists" should "find single result into JSON serialized form" in {
    val response = mockResponse(storeJson)
    jsonpJsonPath("$.street").find.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some("""{"book":"On the street"}"""),
      None
    )
  }
}
