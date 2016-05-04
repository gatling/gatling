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
package io.gatling.http.check.body

import java.nio.charset.StandardCharsets._

import scala.collection.mutable

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.core.CoreDsl
import io.gatling.core.check.CheckResult
import io.gatling.core.check.extractor.jsonpath.JsonFilter
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.HttpDsl
import io.gatling.http.response.{ Response, StringResponseBody }

import org.mockito.Mockito._

class HttpBodyJsonPathCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl {

  implicit val configuration = GatlingConfiguration.loadForTest()

  implicit def cache: mutable.Map[Any, Any] = mutable.Map.empty
  val session = Session("mockSession", 0)

  private def mockResponse(body: String) = {
    val response = mock[Response]
    when(response.body) thenReturn new StringResponseBody(body, UTF_8)
    response
  }

  private val storeJson = """{ "store": {
                            |    "book": "In store"
                            |  },
                            |  "street": {
                            |    "book": "On the street"
                            |  }
                            |}""".stripMargin

  "jsonPath.find.exists" should "find single result into JSON serialized form" in {
    val response = mockResponse(storeJson)
    jsonPath("$.street").find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some("""{"book":"On the street"}"""), None)
  }

  it should "find single result into Map object form" in {
    val response = mockResponse(storeJson)
    jsonPath("$.street").ofType[Map[String, Any]].find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(Map("book" -> "On the street")), None)
  }

  private def testNullAttributeValue[X: JsonFilter]: Unit = {
    val response = mockResponse("""{"foo": null}""")
    jsonPath("$.foo").ofType[X].find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "find a null attribute value" in {
    testNullAttributeValue[String]
    testNullAttributeValue[Any]
    testNullAttributeValue[Int]
    testNullAttributeValue[Seq[Any]]
    testNullAttributeValue[Map[String, Any]]
  }
}
