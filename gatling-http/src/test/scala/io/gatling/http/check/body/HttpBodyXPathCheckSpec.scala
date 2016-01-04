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
import scala.xml.Elem

import org.mockito.Mockito._

import io.gatling.{ ValidationValues, BaseSpec }
import io.gatling.core.CoreDsl
import io.gatling.core.check.CheckResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.HttpDsl
import io.gatling.http.response.{ Response, StringResponseBody }

class HttpBodyXPathCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl {

  implicit val configuration = GatlingConfiguration.loadForTest()

  implicit def cache: mutable.Map[Any, Any] = mutable.Map.empty
  val session = Session("mockSession", 0)

  def mockResponse(xml: Elem): Response = {
    val response = mock[Response]
    when(response.body) thenReturn new StringResponseBody(xml.toString(), UTF_8)
    when(response.hasResponseBody) thenReturn true
    response
  }

  "xpath.find.exists" should "find single result" in {

    val response = mockResponse(<id>1072920417</id>)

    xpath("/id", Nil).find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some("1072920417"), None)
  }

  it should "find first occurrence" in {

    val response = mockResponse(<root>
                                  <id>1072920417</id><id>1072920418</id>
                                </root>)

    xpath("//id").find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some("1072920417"), None)
  }

  "xpath.findAll.exists" should "find all occurrences" in {

    val response = mockResponse(<root>
                                  <id>1072920417</id><id>1072920418</id>
                                </root>)

    xpath("//id").findAll.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(Seq("1072920417", "1072920418")), None)
  }

  it should "fail when finding nothing instead of returning an empty Seq" in {

    val response = mockResponse(<root>
                                  <id>1072920417</id><id>1072920418</id>
                                </root>)

    xpath("//fo").findAll.exists.build.check(response, session).failed shouldBe "xpath((//fo,List())).findAll.exists, found nothing"
  }

  "xpath.count.exists" should "find all occurrences" in {

    val response = mockResponse(<root>
                                  <id>1072920417</id><id>1072920418</id>
                                </root>)

    xpath("//id").count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(2), None)
  }

  it should "return 0 when finding nothing instead of failing" in {

    val response = mockResponse(<root>
                                  <id>1072920417</id><id>1072920418</id>
                                </root>)

    xpath("//fo").count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(0), None)
  }
}
