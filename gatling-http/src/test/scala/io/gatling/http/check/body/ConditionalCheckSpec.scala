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
import io.gatling.commons.validation.Success
import io.gatling.core.CoreDsl
import io.gatling.core.EmptySession
import io.gatling.core.check.{ Check, CheckResult }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response

class ConditionalCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl with EmptySession {

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  "checkIf.true.succeed" should "perform the succeed nested check" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val thenCheck: HttpCheck = substring(""""id":"""").count
    val check: HttpCheck = checkIf((_: Response, _: Session) => Success(true))(thenCheck)
    check.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(Some(2), None)
  }

  "checkIf.true.failed" should "perform the failed nested check" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val substringValue = """"foo":""""
    val thenCheck: HttpCheck = substring(substringValue).findAll.exists
    val check: HttpCheck = checkIf((_: Response, _: Session) => Success(true))(thenCheck)
    check.check(response, emptySession, Check.newPreparedCache).failed shouldBe s"substring($substringValue).findAll.exists, found nothing"
  }

  "checkIf.false.succeed" should "not perform the succeed nested check" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val thenCheck: HttpCheck = substring(""""id":"""").count
    val check: HttpCheck = checkIf((_: Response, _: Session) => Success(false))(thenCheck)
    check.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
  }

  "checkIf.false.failed" should "not perform the failed nested check" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val substringValue = """"foo":""""
    val thenCheck: HttpCheck = substring(substringValue).findAll.exists
    val check: HttpCheck = checkIf((_: Response, _: Session) => Success(false))(thenCheck)
    check.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(None, None)
  }

}
