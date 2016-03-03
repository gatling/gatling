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

import org.mockito.Mockito._

import io.gatling.{ ValidationValues, BaseSpec }
import io.gatling.core.CoreDsl
import io.gatling.core.check.CheckResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.http.HttpDsl
import io.gatling.http.response.{ StringResponseBody, Response }

import scala.collection.mutable

import io.gatling.commons.validation.Success
import io.gatling.core.check.ConditionalCheck._
import io.gatling.http.Predef._
import io.gatling.http.check.HttpCheck

class ConditionalCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl {

  implicit val configuration = GatlingConfiguration.loadForTest()

  implicit def cache: mutable.Map[Any, Any] = mutable.Map.empty
  val session = Session("mockSession", 0)

  private def mockResponse(body: String) = {
    val response = mock[Response]
    when(response.body) thenReturn new StringResponseBody(body, UTF_8)
    response
  }

  "checkIf.true.succeed" should "perform the succeed nested check" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val thenCheck: HttpCheck = substring(""""id":"""").count
    val check: HttpCheck = checkIf((r: Response, s: Session) => Success(true))(thenCheck)
    check.check(response, session).succeeded shouldBe CheckResult(Some(2), None)
  }

  "checkIf.true.failed" should "perform the failed nested check" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val substringValue = """"foo":""""
    val thenCheck: HttpCheck = substring(substringValue).findAll.exists
    val check: HttpCheck = checkIf((r: Response, s: Session) => Success(true))(thenCheck)
    check.check(response, session).failed shouldBe s"substring($substringValue).findAll.exists, found nothing"
  }

  "checkIf.false.succeed" should "not perform the succeed nested check" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val thenCheck: HttpCheck = substring(""""id":"""").count
    val check: HttpCheck = checkIf((r: Response, s: Session) => Success(false))(thenCheck)
    check.check(response, session).succeeded shouldBe CheckResult(None, None)
  }

  "checkIf.false.failed" should "not perform the failed nested check" in {
    val response = mockResponse("""[{"id":"1072920417"},"id":"1072920418"]""")
    val substringValue = """"foo":""""
    val thenCheck: HttpCheck = substring(substringValue).findAll.exists
    val check: HttpCheck = checkIf((r: Response, s: Session) => Success(false))(thenCheck)
    check.check(response, session).succeeded shouldBe CheckResult(None, None)
  }

  "checkIfOrElse.true.succeed" should "perform the succeed then nested check" in {
    val response = mockResponse("""[{"then":"1072920417"},"then":"1072920418","else":"1072920442"]""")
    val thenCheck: HttpCheck = substring(""""then":"""").count
    val elseCheck: HttpCheck = substring(""""else":"""").count
    val check: HttpCheck = checkIfOrElse((r: Response, s: Session) => Success(true))(thenCheck)(elseCheck)
    check.check(response, session).succeeded shouldBe CheckResult(Some(2), None)
  }

  "checkIfOrElse.true.failed" should "perform the failed then nested check" in {
    val response = mockResponse("""[{"then":"1072920417"},"then":"1072920418","else":"1072920442"]""")
    val thenSubstringValue = """"!then":""""
    val elseSubstringValue = """"!else":""""
    val thenCheck: HttpCheck = substring(thenSubstringValue).findAll.exists
    val elseCheck: HttpCheck = substring(elseSubstringValue).findAll.exists
    val check: HttpCheck = checkIfOrElse((r: Response, s: Session) => Success(true))(thenCheck)(elseCheck)
    check.check(response, session).failed shouldBe s"substring($thenSubstringValue).findAll.exists, found nothing"
  }

  "checkIfOrElse.false.succeed" should "perform the succeed else nested check" in {
    val response = mockResponse("""[{"then":"1072920417"},"then":"1072920418","else":"1072920442"]""")
    val thenCheck: HttpCheck = substring(""""then":"""").count
    val elseCheck: HttpCheck = substring(""""else":"""").count
    val check: HttpCheck = checkIfOrElse((r: Response, s: Session) => Success(false))(thenCheck)(elseCheck)
    check.check(response, session).succeeded shouldBe CheckResult(Some(1), None)
  }

  "checkIfOrElse.false.failed" should "perform the failed else nested check" in {
    val response = mockResponse("""[{"then":"1072920417"},"then":"1072920418","else":"1072920442"]""")
    val thenSubstringValue = """"!then":""""
    val elseSubstringValue = """"!else":""""
    val thenCheck: HttpCheck = substring(thenSubstringValue).findAll.exists
    val elseCheck: HttpCheck = substring(elseSubstringValue).findAll.exists
    val check: HttpCheck = checkIfOrElse((r: Response, s: Session) => Success(false))(thenCheck)(elseCheck)
    check.check(response, session).failed shouldBe s"substring($elseSubstringValue).findAll.exists, found nothing"
  }

}
