/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check.body

import java.nio.charset.StandardCharsets._

import io.gatling.core.check.CheckResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.core.test.ValidationValues
import io.gatling.http.response.{ Response, StringResponseBody }
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.mutable

class HttpBodySubstringCheckSpec extends FlatSpec with Matchers with ValidationValues with MockitoSugar {

  GatlingConfiguration.setUpForTest()

  implicit def cache = mutable.Map.empty[Any, Any]

  def mockResponse(body: String): Response = {
    val response = mock[Response]
    when(response.body) thenReturn StringResponseBody(body, UTF_8)
    response
  }

  val session = Session("mockSession", "mockUserName")

  "substring.find.exists" should "find single result" in {
    val response = mockResponse("""{"id":"1072920417"}""")
    HttpBodySubstringCheckBuilder.substring(""""id":"""".el).find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(1), None)
  }

  it should "find first occurrence" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    HttpBodySubstringCheckBuilder.substring(""""id":"""".el).find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(2), None)
  }

  "substring.findAll.exists" should "find all occurrences" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    HttpBodySubstringCheckBuilder.substring(""""id":"""".el).findAll.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(Seq(2, 21)), None)
  }

  it should "fail when finding nothing instead of returning an empty Seq" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val substringValue = """"foo":""""
    HttpBodySubstringCheckBuilder.substring(substringValue.el).findAll.exists.build.check(response, session).failed shouldBe s"substring($substringValue).findAll.exists, found nothing"
  }

  "substring.count.exists" should "find all occurrences" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    HttpBodySubstringCheckBuilder.substring(""""id":"""".el).count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(2), None)
  }

  it should "return 0 when finding nothing instead of failing" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    HttpBodySubstringCheckBuilder.substring(""""foo":"""".el).count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(0), None)
  }
}
