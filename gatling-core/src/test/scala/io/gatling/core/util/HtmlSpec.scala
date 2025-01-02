/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.core.util

import io.gatling.BaseSpec
import io.gatling.core.util.Html._

class HtmlSpec extends BaseSpec {
  "Html.unescape" should "return input when it doesn't contain any entity" in {
    unescape("foo bar") shouldBe "foo bar"
  }

  it should "escape an entity when it's in the middle of the input" in {
    unescape("foo &eacute; bar") shouldBe "foo é bar"
  }

  it should "escape an entity when it's at the beginning of the input" in {
    unescape("&eacute; foo bar") shouldBe "é foo bar"
  }

  it should "escape an entity when it's at the end of the input" in {
    unescape("foo bar &eacute;") shouldBe "foo bar é"
  }

  it should "ignore invalid entity" in {
    unescape("foo &invalid &eacute; bar") shouldBe "foo &invalid é bar"
  }

  it should "support entity number" in {
    unescape("foo &#233; bar") shouldBe "foo é bar"
  }

  it should "ignore invalid entity number" in {
    unescape("foo &#; bar") shouldBe "foo &#; bar"
  }

  it should "support hex with complement entity number" in {
    unescape("foo &#x00E9; bar") shouldBe "foo é bar"
  }

  it should "ignore invalid entity hex number" in {
    unescape("foo &#x; bar") shouldBe "foo &#x; bar"
  }
}
