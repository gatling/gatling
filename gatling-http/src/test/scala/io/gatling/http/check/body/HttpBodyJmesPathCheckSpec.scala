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
import io.gatling.commons.validation.Failure
import io.gatling.core.CoreDsl
import io.gatling.core.EmptySession
import io.gatling.core.check.{ Check, CheckMaterializer, CheckResult }
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response

import com.fasterxml.jackson.databind.JsonNode

class HttpBodyJmesPathCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl with EmptySession {

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val materializer: CheckMaterializer[JmesPathCheckType, HttpCheck, Response, JsonNode] =
    HttpBodyJmesPathCheckMaterializer.instance(new JsonParsers)

  private val storeJson = """{ "store": {
                            |    "book": "In store"
                            |  },
                            |  "street": {
                            |    "book": "On the street"
                            |  }
                            |}""".stripMargin

  "jmesPath.find" should "support long values" in {
    val response = mockResponse(s"""{"value": ${Long.MaxValue}}""")
    jmesPath("value").ofType[Long].find.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(Long.MaxValue), None)
  }

  "jmesPath.find.exists" should "find single result into JSON serialized form" in {
    val response = mockResponse(storeJson)
    jmesPath("street").find.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some("""{"book":"On the street"}"""),
      None
    )
  }

  it should "fail when the path doesn't match" in {
    val response = mockResponse(storeJson)
    jmesPath("foo").find.exists.check(response, emptySession, Check.newPreparedCache) shouldBe a[Failure]
  }

  it should "fail when the attribute value is null" in {
    val response = mockResponse("""{"foo": null}""")
    jmesPath("foo").ofType[String].find.exists.check(response, emptySession, Check.newPreparedCache) shouldBe a[Failure]
  }

  it should "find single result into Map object form" in {
    val response = mockResponse(storeJson)
    jmesPath("street").ofType[Map[String, Any]].find.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Map("book" -> "On the street")),
      None
    )
  }

  it should "succeed when expecting a non-null value and getting a non-null one" in {
    val response = mockResponse("""{"foo": "bar"}""")
    jmesPath("foo").ofType[Any].find.notNull.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some("bar"), None)
  }

  it should "fail when expecting a non-null value and getting a null one" in {
    val response = mockResponse("""{"foo": null}""")
    jmesPath("foo").find
      .check(response, emptySession, Check.newPreparedCache) shouldBe a[Failure]
  }

  it should "not fail on empty array" in {
    val response = mockResponse("""{"documents":[]}""")
    jmesPath("documents").ofType[Seq[_]].find.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Vector.empty),
      None
    )
  }
}
