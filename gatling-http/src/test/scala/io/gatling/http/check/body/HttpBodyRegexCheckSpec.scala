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
import org.mockito.Mockito._
import org.scalatest.{ Matchers, FlatSpec }
import org.scalatest.mock.MockitoSugar

import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.core.test.ValidationValues
import io.gatling.http.response.{ StringResponseBody, Response }

import scala.collection.mutable

class HttpBodyRegexCheckSpec extends FlatSpec with Matchers with ValidationValues with MockitoSugar {

  GatlingConfiguration.setUpForTest()

  implicit def cache = mutable.Map.empty[Any, Any]

  val session = Session("mockSession", "mockUserName")

  "regex.find.exists" should "find single result" in {

    val response = mock[Response]
    when(response.body) thenReturn StringResponseBody(""""{"id":"1072920417"}"""", UTF_8)

    HttpBodyRegexCheckBuilder.regex(""""id":"(.+?)"""".el).find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some("1072920417"), None)
  }

  it should "find first occurrence" in {

    val response = mock[Response]
    when(response.body) thenReturn StringResponseBody(""""[{"id":"1072920417"},"id":"1072920418"]"""", UTF_8)

    HttpBodyRegexCheckBuilder.regex(""""id":"(.+?)"""".el).find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some("1072920417"), None)
  }

  "regex.findAll.exists" should "find all occurrences" in {

    val response = mock[Response]
    when(response.body) thenReturn StringResponseBody(""""[{"id":"1072920417"},"id":"1072920418"]"""", UTF_8)

    HttpBodyRegexCheckBuilder.regex(""""id":"(.+?)"""".el).findAll.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(Seq("1072920417", "1072920418")), None)
  }

  it should "fail when finding nothing instead of returning an empty Seq" in {

    val response = mock[Response]
    when(response.body) thenReturn StringResponseBody(""""[{"id":"1072920417"},"id":"1072920418"]"""", UTF_8)
    val regexValue = """"foo":"(.+?)""""

    HttpBodyRegexCheckBuilder.regex(regexValue.el).findAll.exists.build.check(response, session).failed shouldBe s"regex($regexValue).findAll.exists, found nothing"
  }

  "regex.count.exists" should "find all occurrences" in {

    val response = mock[Response]
    when(response.body) thenReturn StringResponseBody(""""[{"id":"1072920417"},"id":"1072920418"]"""", UTF_8)

    HttpBodyRegexCheckBuilder.regex(""""id":"(.+?)"""".el).count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(2), None)
  }

  it should "return 0 when finding nothing instead of failing" in {

    val response = mock[Response]
    when(response.body) thenReturn StringResponseBody(""""[{"id":"1072920417"},"id":"1072920418"]"""", UTF_8)
    val regexValue = """"foo":"(.+?)""""

    HttpBodyRegexCheckBuilder.regex(regexValue.el).count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(0), None)
  }
}
