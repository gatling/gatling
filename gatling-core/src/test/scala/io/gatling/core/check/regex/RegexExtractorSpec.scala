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

package io.gatling.core.check.regex

import io.gatling.{ BaseSpec, ValidationValues }

class RegexExtractorSpec extends BaseSpec with ValidationValues {

  private val patterns = new Patterns(Long.MaxValue)

  "count" should "return Some(0) when no results" in {
    val stringRegexExtractor = RegexExtractors.count("regex", """foo""", patterns)
    stringRegexExtractor("""{"id":"1072920417","result":"[{\"SearchDefinitionID\":116},{\"SearchDefinitionID\":108}]","error":null}""").succeeded shouldBe Some(
      0
    )
  }

  "findAll" should "return expected result with anywhere expression" in {
    val stringRegexExtractor = RegexExtractors.findAll[String]("regex", """"SearchDefinitionID\\":(\d*)""", patterns)
    stringRegexExtractor("""{"id":"1072920417","result":"[{\"SearchDefinitionID\":116},{\"SearchDefinitionID\":108}]","error":null}""").succeeded shouldBe Some(
      List("116", "108")
    )
  }
}
