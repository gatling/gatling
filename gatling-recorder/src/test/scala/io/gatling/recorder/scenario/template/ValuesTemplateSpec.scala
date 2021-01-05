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

package io.gatling.recorder.scenario.template

import io.gatling.BaseSpec

class ValuesTemplateSpec extends BaseSpec {

  private def str(s: String) = s.replaceAll("""\r\n""", "\n")

  "values template" should "generate empty string if no variables" in {
    val res = ValuesTemplate.render(Seq.empty)
    res.toString shouldBe empty
  }

  it should "list variables" in {
    val res = str(ValuesTemplate.render(Seq(new Value("n1", "v1"), new Value("n2", "v2"))))
    val expected = str(s"""    val n1 = ${protectWithTripleQuotes("v1")}
    val n2 = ${protectWithTripleQuotes("v2")}""")

    res shouldBe expected
  }
}
