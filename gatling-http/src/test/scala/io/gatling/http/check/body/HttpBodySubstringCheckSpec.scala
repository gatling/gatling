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
import io.gatling.core.CoreDsl
import io.gatling.core.EmptySession
import io.gatling.core.check.{ Check, CheckMaterializer, CheckResult }
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response

class HttpBodySubstringCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl with EmptySession {

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val materializer: CheckMaterializer[SubstringCheckType, HttpCheck, Response, String] =
    HttpBodySubstringCheckMaterializer.Instance

  "substring.find.exists" should "find single result" in {
    val response = mockResponse("""{"id":"1072920417"}""")
    substring(""""id":"""").find.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(1), None)
  }

  it should "find first occurrence" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    substring(""""id":"""").find.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(2), None)
  }

  "substring.findAll.exists" should "find all occurrences" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    substring(""""id":"""").findAll.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(Seq(2, 21)), None)
  }

  it should "fail when finding nothing instead of returning an empty Seq" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val substringValue = """"foo":""""
    substring(substringValue).findAll.exists
      .check(response, emptySession, Check.newPreparedCache)
      .failed shouldBe s"substring($substringValue).findAll.exists, found nothing"
  }

  "substring.count.exists" should "find all occurrences" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    substring(""""id":"""").count.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(2), None)
  }

  it should "return 0 when finding nothing instead of failing" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    substring(""""foo":"""").count.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(0), None)
  }
}
