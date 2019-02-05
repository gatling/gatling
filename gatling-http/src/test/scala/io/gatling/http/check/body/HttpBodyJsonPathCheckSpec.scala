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

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.core.CoreDsl
import io.gatling.core.check.extractor.jsonpath.JsonPathCheckType
import io.gatling.core.check.{ CheckMaterializer, CheckResult }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers
import io.gatling.core.session._
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.{ CharArrayResponseBody, Response }

import org.mockito.Mockito._
import org.scalatest.matchers.{ MatchResult, Matcher }

class HttpBodyJsonPathCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl {

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val materializer: CheckMaterializer[JsonPathCheckType, HttpCheck, Response, Any] = new HttpBodyJsonPathCheckMaterializer(JsonParsers())

  implicit def cache: JHashMap[Any, Any] = new JHashMap
  val session = Session("mockSession", 0, System.currentTimeMillis())

  private def mockResponse(body: String) = {
    val response = mock[Response]
    when(response.body) thenReturn new CharArrayResponseBody(body.toCharArray, UTF_8)
    response
  }

  private val storeJson = """{ "store": {
                            |    "book": "In store"
                            |  },
                            |  "street": {
                            |    "book": "On the street"
                            |  }
                            |}""".stripMargin

  "jsonPath.find" should "support long values" in {
    val response = mockResponse(s"""{"value": ${Long.MaxValue}}""")
    jsonPath("$.value").ofType[Long].find.exists.check(response, session).succeeded shouldBe CheckResult(Some(Long.MaxValue), None)
  }

  "jsonPath.find.exists" should "find single result into JSON serialized form" in {
    val response = mockResponse(storeJson)
    jsonPath("$.street").find.exists.check(response, session).succeeded shouldBe CheckResult(Some("""{"book":"On the street"}"""), None)
  }

  it should "find single result into Map object form" in {
    val response = mockResponse(storeJson)
    jsonPath("$.street").ofType[Map[String, Any]].find.exists.check(response, session).succeeded shouldBe CheckResult(Some(Map("book" -> "On the street")), None)
  }

  it should "find a null attribute value when expected type is String" in {
    val response = mockResponse("""{"foo": null}""")
    jsonPath("$.foo").ofType[String].find.exists.check(response, session).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "find a null attribute value when expected type is Any" in {
    val response = mockResponse("""{"foo": null}""")
    jsonPath("$.foo").ofType[Any].find.exists.check(response, session).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "find a null attribute value when expected type is Int" in {
    val response = mockResponse("""{"foo": null}""")
    jsonPath("$.foo").ofType[Int].find.exists.check(response, session).succeeded shouldBe CheckResult(Some(null.asInstanceOf[Int]), None)
  }

  it should "find a null attribute value when expected type is Seq" in {
    val response = mockResponse("""{"foo": null}""")
    jsonPath("$.foo").ofType[Seq[Any]].find.exists.check(response, session).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "find a null attribute value when expected type is Map" in {
    val response = mockResponse("""{"foo": null}""")
    jsonPath("$.foo").ofType[Map[String, Any]].find.exists.check(response, session).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "succeed when expecting a null value and getting a null one" in {
    val response = mockResponse("""{"foo": null}""")
    jsonPath("$.foo").ofType[Any].find.isNull.check(response, session).succeeded shouldBe CheckResult(Some(null), None)
  }

  it should "fail when expecting a null value and getting a non-null one" in {
    val response = mockResponse("""{"foo": "bar"}""")
    jsonPath("$.foo").ofType[Any].find.isNull.check(response, session).failed shouldBe "jsonPath($.foo).find.isNull, but actually found bar"
  }

  it should "succeed when expecting a non-null value and getting a non-null one" in {
    val response = mockResponse("""{"foo": "bar"}""")
    jsonPath("$.foo").ofType[Any].find.notNull.check(response, session).succeeded shouldBe CheckResult(Some("bar"), None)
  }

  it should "fail when expecting a non-null value and getting a null one" in {
    val response = mockResponse("""{"foo": null}""")
    jsonPath("$.foo").ofType[Any].find.notNull.check(response, session).failed shouldBe "jsonPath($.foo).find.notNull, but actually found null"
  }

  "jsonPath.findAll.exists" should "fetch all matches" in {
    val response = mockResponse(storeJson)
    jsonPath("$..book").findAll.exists.check(response, session).succeeded shouldBe CheckResult(Some(Seq("In store", "On the street")), None)
  }

  private def beIn[T](seq: Seq[T]) =
    new Matcher[T] {
      def apply(left: T) =
        MatchResult(
          seq.contains(left),
          s"$left was not in $seq",
          s"$left was in $seq"
        )
    }

  "jsonPath.findRandom.exists" should "fetch a single random match" in {
    val response = mockResponse(storeJson)
    jsonPath("$..book").findRandom.exists.check(response, session).succeeded.extractedValue.get.asInstanceOf[String] should beIn(Seq("In store", "On the street"))
  }

  it should "fetch at max num results" in {
    val response = mockResponse(storeJson)
    jsonPath("$..book").findRandom(1).exists.check(response, session).succeeded.extractedValue.get.asInstanceOf[Seq[String]] should beIn(Seq(Seq("In store"), Seq("On the street")))
  }

  it should "fetch all the matches when expected number is greater" in {
    val response = mockResponse(storeJson)
    jsonPath("$..book").findRandom(3).exists.check(response, session).succeeded shouldBe CheckResult(Some(Seq("In store", "On the street")), None)
  }

  it should "fail when failIfLess is enabled and expected number is greater" in {
    val response = mockResponse(storeJson)
    jsonPath("$..book").findRandom(3, failIfLess = true).exists.check(response, session).failed
  }
}
