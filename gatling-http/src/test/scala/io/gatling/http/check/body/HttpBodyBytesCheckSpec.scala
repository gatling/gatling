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

import java.nio.charset.StandardCharsets._

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.core.CoreDsl
import io.gatling.core.EmptySession
import io.gatling.core.check.{ Check, CheckMaterializer, CheckResult }
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response

class HttpBodyBytesCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl with EmptySession {

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val materializer: CheckMaterializer[BodyBytesCheckType, HttpCheck, Response, Array[Byte]] = HttpBodyBytesCheckMaterializer.Instance

  "bodyBytes.find.is" should "support byte arrays equality" in {
    val string = "Hello World"
    val responseBytes = string.getBytes(UTF_8)
    val response = mockResponse(responseBytes)
    bodyBytes.find.is(string.getBytes(UTF_8)).check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(responseBytes), None)
  }

  it should "fail when byte arrays are different" in {
    val string = "Hello World"
    val responseBytes = string.getBytes(UTF_8)
    val response = mockResponse(responseBytes)
    bodyBytes.find.is("HELLO WORLD".getBytes(UTF_8)).check(response, emptySession, Check.newPreparedCache).failed shouldBe a[String]
  }
}
