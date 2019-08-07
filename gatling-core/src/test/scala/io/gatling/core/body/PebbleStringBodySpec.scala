/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.core.body

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.{ BaseSpec, ValidationValues }

class PebbleStringBodySpec extends BaseSpec with ValidationValues {

  private implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private def newSession(contents: Map[String, Any]) =
    Session("scenario", 0, System.currentTimeMillis(), contents)

  "Static String" should "return itself" in {
    val session = newSession(Map.empty)
    val body = PebbleStringBody("bar")
    body(session).succeeded shouldBe "bar"
  }

  it should "return empty when empty" in {
    val session = newSession(Map.empty)
    val body = PebbleStringBody("")
    body(session).succeeded shouldBe ""
  }

  "One monovalued Expression" should "return expected result when the variable is the whole string" in {
    val session = newSession(Map("bar" -> "BAR"))
    val body = PebbleStringBody("{{bar}}")
    body(session).succeeded shouldBe "BAR"
  }

  it should "return expected result when the variable is at the end of the string" in {
    val session = newSession(Map("bar" -> "BAR"))
    val body = PebbleStringBody("foo{{bar}}")
    body(session).succeeded shouldBe "fooBAR"
  }

  it should "return expected result when the variable is at the beginning of the string" in {
    val session = newSession(Map("bar" -> "BAR"))
    val body = PebbleStringBody("{{bar}}foo")
    body(session).succeeded shouldBe "BARfoo"
  }

  it should "return expected result when the variable is in the middle of the string" in {
    val session = newSession(Map("bar" -> "BAR"))
    val body = PebbleStringBody("foo{{bar}}foo")
    body(session).succeeded shouldBe "fooBARfoo"
  }

  it should "handle when an attribute is missing" in {
    val session = newSession(Map("foo" -> "FOO"))
    val body = PebbleStringBody("foo{{bar}}")
    body(session).succeeded shouldBe "foo"
  }

  it should "properly handle multiline JSON template" in {
    val session = newSession(Map("foo" -> "FOO"))
    val body = PebbleStringBody("""{
        "foo": {{foo}},
        "bar": 1
      }""")
    body(session).succeeded shouldBe """{
        "foo": FOO,
        "bar": 1
      }"""
  }

  it should "properly handle if" in {
    val session = newSession(Map("barTrue" -> "BARTRUE", "barFalse" -> "BARFALSE", "true" -> true, "false" -> false))
    val body = PebbleStringBody("{%if true %}{{barTrue}}{%endif%}{%if false %}{{barFalse}}{% endif %}")
    body(session).succeeded shouldBe "BARTRUE"
  }

  it should "handle gracefully when an exception is thrown" in {
    val session = newSession(Map.empty)
    val body = PebbleStringBody("{{ 0 / 0 }}")
    body(session).failed
  }

  "Multivalued Expression" should "return expected result with 2 monovalued expressions" in {
    val session = newSession(Map("foo" -> "FOO", "bar" -> "BAR"))
    val body = PebbleStringBody("{{foo}} {{bar}}")
    body(session).succeeded shouldBe "FOO BAR"
  }

  it should "properly handle for loop" in {
    val session = newSession(Map("list" -> List("hello", "bonjour", 42)))
    val body = PebbleStringBody("{% for value in list %}{{value }}{% endfor %}")
    body(session).succeeded shouldBe "hellobonjour42"
  }

  it should "return expected result when using filters" in {
    val session = newSession(Map("bar" -> "bar"))
    val body = PebbleStringBody("{{ bar | capitalize }}{% filter upper %}hello{% endfilter %}")
    body(session).succeeded shouldBe "BarHELLO"
  }
}
