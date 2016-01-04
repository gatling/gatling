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

import org.mockito.Mockito._

import io.gatling.{ ValidationValues, BaseSpec }
import io.gatling.core.CoreDsl
import io.gatling.core.check.CheckResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheckSupport
import io.gatling.http.response.{ StringResponseBody, Response }

class HttpBodyRegexCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl {

  object RegexSupport extends HttpCheckSupport
  val regexCheck = RegexSupport.regex _

  implicit val configuration = GatlingConfiguration.loadForTest()

  implicit def cache: mutable.Map[Any, Any] = mutable.Map.empty
  val session = Session("mockSession", 0)

  private def mockResponse(body: String) = {
    val response = mock[Response]
    when(response.body) thenReturn new StringResponseBody(body, UTF_8)
    response
  }

  "regex.find.exists" should "find single result" in {
    val response = mockResponse("""{"id":"1072920417"}""")
    regexCheck(""""id":"(.+?)"""").find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some("1072920417"), None)
  }

  it should "find first occurrence" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    regexCheck(""""id":"(.+?)"""").find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some("1072920417"), None)
  }

  "regex.findAll.exists" should "find all occurrences" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    regexCheck(""""id":"(.+?)"""").findAll.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(Seq("1072920417", "1072920418")), None)
  }

  it should "fail when finding nothing instead of returning an empty Seq" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val regexValue = """"foo":"(.+?)""""
    regexCheck(regexValue).findAll.exists.build.check(response, session).failed shouldBe s"regex($regexValue).findAll.exists, found nothing"
  }

  it should "fail with expected message when transforming" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val regexValue = """"foo":"(.+?)""""
    regexCheck(regexValue).findAll.transform(_.map(_ + "foo")).exists.build.check(response, session).failed shouldBe s"regex($regexValue).findAll.transform.exists, found nothing"
  }

  "regex.count.exists" should "find all occurrences" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    regexCheck(""""id":"(.+?)"""").count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(2), None)
  }

  it should "return 0 when finding nothing instead of failing" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val regexValue = """"foo":"(.+?)""""
    regexCheck(regexValue).count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(0), None)
  }
}
