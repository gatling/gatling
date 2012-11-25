/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.session

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.core.session.ELParser.parseEL

@RunWith(classOf[JUnitRunner])
class ELParserSpec extends Specification {

	"parseEL" should {

		"return expected result with 1 monovalued expression that is the whole string" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			parseEL("${bar}")(session) must beEqualTo("BAR")
		}

		"return expected result with 1 monovalued expression at the end of the string" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			parseEL("foo${bar}")(session) must beEqualTo("fooBAR")
		}

		"return expected result with 1 monovalued expression at the beginning of the string" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			parseEL("${bar}baz")(session) must beEqualTo("BARbaz")
		}

		"return expected result with 1 monovalued expression in the middle of the string" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			parseEL("foo${bar}baz")(session) must beEqualTo("fooBARbaz")
		}

		"return expected result with 2 monovalued expressions" in {
			val session = new Session("scenario", 1, Map("foo" -> "FOO", "bar" -> "BAR"))
			parseEL("${foo} ${bar}")(session) must beEqualTo("FOO BAR")
		}

		"handle gracefully monovalued expression with missing attribute" in {
			val session = new Session("scenario", 1, Map.empty)
			parseEL("foo${bar}")(session) must beEqualTo("foo")
		}

		"return expected result with multivalued expression with static index" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			parseEL("foo${bar(1)}")(session) must beEqualTo("fooBAR2")
		}

		"return expected result with multivalued expression with resolved index" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2"), "baz" -> 1))
			parseEL("{foo${bar(baz)}}")(session) must beEqualTo("{fooBAR2}")
		}

		"handle gracefully multivalued expression with static index and missing attribute" in {
			val session = new Session("scenario", 1, Map.empty)
			parseEL("foo${bar(1)}")(session) must beEqualTo("foo")
		}

		"handle gracefully multivalued expression with static index and empty attribute" in {
			val session = new Session("scenario", 1, Map("bar" -> Nil))
			parseEL("foo${bar(1)}")(session) must beEqualTo("foo")
		}

		"handle gracefully multivalued expression with static index and missing index" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1")))
			parseEL("foo${bar(1)}")(session) must beEqualTo("foo")
		}

		"handle gracefully multivalued expression with missing resolved index attribute" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			parseEL("{foo${bar(baz)}}")(session) must beEqualTo("{foo}")
		}

		"return correct size for non empty seq" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			parseEL("${bar.size()}")(session) must beEqualTo("2")
		}

		"return correct size for empty seq" in {
			val session = new Session("scenario", 1, Map("bar" -> List()))
			parseEL("${bar.size()}")(session) must beEqualTo("0")
		}

		"return 0 size for missing attribute" in {
			val session = new Session("scenario", 1, Map())
			parseEL("${bar.size()}")(session) must beEqualTo("0")
		}
	}
}