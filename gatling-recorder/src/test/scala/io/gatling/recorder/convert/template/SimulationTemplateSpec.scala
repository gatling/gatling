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

package io.gatling.recorder.convert.template

import io.gatling.BaseSpec

class SimulationTemplateSpec extends BaseSpec {

  "renderNonBaseUrls template" should "generate empty string if no variables" in {
    SimulationTemplate.renderNonBaseUrls(Nil) shouldBe empty
  }

  it should "list variables" in {
    val raw = SimulationTemplate.renderNonBaseUrls(Seq(UrlVal("name1", "url1"), UrlVal("name2", "url2")))
    raw.linesIterator.map(_.trim).toList shouldBe List(
      s"""val name1 = ${protectWithTripleQuotes("url1")}""",
      s"""val name2 = ${protectWithTripleQuotes("url2")}"""
    )
  }
}
