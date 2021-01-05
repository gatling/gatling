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
import io.gatling.core.{ CoreDsl, EmptySession }
import io.gatling.core.check.{ Check, CheckMaterializer, CheckResult }
import io.gatling.core.check.css.{ CssCheckType, CssSelectors }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response

import jodd.lagarto.dom.NodeSelector

class HttpBodyCssCheckSpec extends BaseSpec with ValidationValues with CoreDsl with HttpDsl with EmptySession {

  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private implicit val materializer: CheckMaterializer[CssCheckType, HttpCheck, Response, NodeSelector] =
    HttpBodyCssCheckMaterializer.instance(new CssSelectors(1000))

  "form.find" should "select the first option when none is selected" in {
    val response = mockResponse("""<form name="form">
                                  |  <select name="selectInput">
                                  |    <option value="">Option1</option>
                                  |    <option value="option2Value">Option2</option>
                                  |    <option value="option3Value">Option3</option>
                                  |  </select>
                                  |</form>""".stripMargin)
    form("form[name=form]").find.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Map("selectInput" -> "")),
      None
    )
  }

  it should "select the first selected option" in {
    val response = mockResponse("""<form name="form">
                                  |  <select name="selectInput">
                                  |    <option value="">Option1</option>
                                  |    <option value="option2Value" selected>Option2</option>
                                  |    <option value="option3Value">Option3</option>
                                  |  </select>
                                  |</form>""".stripMargin)
    form("form[name=form]").find.exists.check(response, emptySession, Check.newPreparedCache).succeeded shouldBe CheckResult(
      Some(Map("selectInput" -> "option2Value")),
      None
    )
  }
}
