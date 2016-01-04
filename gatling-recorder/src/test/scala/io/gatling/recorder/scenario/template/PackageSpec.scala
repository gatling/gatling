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
package io.gatling.recorder.scenario.template

import io.gatling.BaseSpec

class PackageSpec extends BaseSpec {

  "protectWithTripleQuotes" should "wrap a String containing double quotes with triple quotes" in {
    val string = "foo\"bar"
    protectWithTripleQuotes(string).mkString shouldBe s"$TripleQuotes$string$TripleQuotes"
  }

  it should "wrap a String containing backslashes with triple quotes" in {
    val string = "foo\\bar"
    protectWithTripleQuotes(string).mkString shouldBe s"$TripleQuotes$string$TripleQuotes"
  }

  it should "otherwise wrap a String with simple quotes" in {
    val string = "foobar"
    protectWithTripleQuotes(string).mkString shouldBe s"$SimpleQuotes$string$SimpleQuotes"
  }
}
