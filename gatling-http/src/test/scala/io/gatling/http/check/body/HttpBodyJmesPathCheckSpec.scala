/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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
import java.util.{ HashMap => JHashMap }

import io.gatling.core.CoreDsl
import io.gatling.core.check.extractor.jmespath.JmesPathCheckType
import io.gatling.core.check.{ CheckMaterializer, CheckResult }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers
import io.gatling.core.session._
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.{ Response, StringResponseBody }
import io.gatling.{ BaseSpec, ValidationValues }

import com.fasterxml.jackson.databind.JsonNode
import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpResponseStatus }

class HttpBodyJmesPathCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl {

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val materializer: CheckMaterializer[JmesPathCheckType, HttpCheck, Response, JsonNode] = new HttpBodyJmesPathCheckMaterializer(JsonParsers())

  val session = Session("mockSession", 0, System.currentTimeMillis())

  private def mockResponse(body: String): Response =
    Response(
      request = null,
      wireRequestHeaders = new DefaultHttpHeaders,
      status = HttpResponseStatus.OK,
      headers = new DefaultHttpHeaders,
      body = new StringResponseBody(body, UTF_8),
      checksums = null,
      bodyLength = 0,
      charset = UTF_8,
      startTimestamp = 0,
      endTimestamp = 0,
      isHttp2 = false
    )

  private val storeJson = """{ "store": {
                            |    "book": "In store"
                            |  },
                            |  "street": {
                            |    "book": "On the street"
                            |  }
                            |}""".stripMargin

  "jmesPath.find" should "support long values" in {
    val response = mockResponse(s"""{"value": ${Long.MaxValue}}""")
    jmesPath("value").ofType[Long].find.exists.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(Long.MaxValue), None)
  }

  "jmesPath.find.exists" should "find single result into JSON serialized form" in {
    val response = mockResponse(storeJson)
    jmesPath("street").find.exists.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some("""{"book":"On the street"}"""), None)
  }

  it should "find single result into Map object form" in {
    val response = mockResponse(storeJson)
    jmesPath("street").ofType[Map[String, Any]].find.exists.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(Map("book" -> "On the street")), None)
  }

  it should "find a null attribute value when expected type is String" in {
    val response = mockResponse("""{"foo": null}""")
    jmesPath("foo").ofType[String].find.exists.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "find a null attribute value when expected type is Any" in {
    val response = mockResponse("""{"foo": null}""")
    jmesPath("foo").ofType[Any].find.exists.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "find a null attribute value when expected type is Int" in {
    val response = mockResponse("""{"foo": null}""")
    jmesPath("foo").ofType[Int].find.exists.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(null.asInstanceOf[Int]), None)
  }

  it should "find a null attribute value when expected type is Seq" in {
    val response = mockResponse("""{"foo": null}""")
    jmesPath("foo").ofType[Seq[Any]].find.exists.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "find a null attribute value when expected type is Map" in {
    val response = mockResponse("""{"foo": null}""")
    jmesPath("foo").ofType[Map[String, Any]].find.exists.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "succeed when expecting a null value and getting a null one" in {
    val response = mockResponse("""{"foo": null}""")
    jmesPath("foo").ofType[Any].find.isNull.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "fail when expecting a null value and getting a non-null one" in {
    val response = mockResponse("""{"foo": "bar"}""")
    jmesPath("foo").ofType[Any].find.isNull.check(response, session, new JHashMap[Any, Any]).failed shouldBe "jmesPath(foo).find.isNull, but actually found bar"
  }

  it should "succeed when expecting a non-null value and getting a non-null one" in {
    val response = mockResponse("""{"foo": "bar"}""")
    jmesPath("foo").ofType[Any].find.notNull.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some("bar"), None)
  }

  it should "fail when expecting a non-null value and getting a null one" in {
    val response = mockResponse("""{"foo": null}""")
    jmesPath("foo").ofType[Any].find.notNull.check(response, session, new JHashMap[Any, Any]).failed shouldBe "jmesPath(foo).find.notNull, but actually found null"
  }

  it should "not fail on empty array" in {
    val response = mockResponse("""{"documents":[]}""")
    jmesPath("documents").ofType[Seq[_]].find.exists.check(response, session, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(Vector.empty), None)
  }
}
