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
import io.gatling.core.check.CheckResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.http.HttpDsl
import io.gatling.http.response.{ ByteArrayResponseBody, Response }
import io.gatling.{ BaseSpec, ValidationValues }
import org.mockito.Mockito._

class HttpBodyBytesCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl {

  implicit val configuration = GatlingConfiguration.loadForTest()
  implicit val materializer = HttpBodyBytesCheckMaterializer

  implicit def cache: JHashMap[Any, Any] = new JHashMap
  val session = Session("mockSession", 0, System.currentTimeMillis())

  private def mockResponse(body: Array[Byte]) = {
    val response = mock[Response]
    when(response.body) thenReturn new ByteArrayResponseBody(body, UTF_8)
    response
  }

  "bodyBytes.find.is" should "support byte arrays equality" in {
    val string = "Hello World"
    val responseBytes = string.getBytes(UTF_8)
    val response = mockResponse(responseBytes)
    bodyBytes.find.is(string.getBytes(UTF_8)).check(response, session).succeeded shouldBe CheckResult(Some(responseBytes), None)
  }

  it should "fail when byte arrays are different" in {
    val string = "Hello World"
    val responseBytes = string.getBytes(UTF_8)
    val response = mockResponse(responseBytes)
    bodyBytes.find.is("HELLO WORLD".getBytes(UTF_8)).check(response, session).failed shouldBe a[String]
  }
}
