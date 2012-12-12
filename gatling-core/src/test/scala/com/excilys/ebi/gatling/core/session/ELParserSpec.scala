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

import scalaz.{ Failure, Success }

@RunWith(classOf[JUnitRunner])
class ELParserSpec extends Specification {

	"One monovalued Expression" should {

		"return expected result when the variable is the whole string" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			val expression = Expression[String]("${bar}")
			expression(session) must beEqualTo(Success("BAR"))
		}

		"return expected result when the variable is at the end of the string" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			val expression = Expression[String]("foo${bar}")
			expression(session) must beEqualTo(Success("fooBAR"))
		}

		"return expected result when the variable is at the beginning of the string" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			val expression = Expression[String]("${bar}baz")
			expression(session) must beEqualTo(Success("BARbaz"))
		}

		"return expected result when the variable is in the middle of the string" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			val expression = Expression[String]("foo${bar}baz")
			expression(session) must beEqualTo(Success("fooBARbaz"))
		}

		"handle gracefully when an attribute is missing" in {
			val session = new Session("scenario", 1, Map("foo" -> "FOO"))
			val expression = Expression[String]("foo${bar}")
			expression(session) must beEqualTo(Failure(undefinedSessionAttributeMessage("bar")))
		}
	}

	"Multivalued Expression" should {

		"return expected result with 2 monovalued expressions" in {
			val session = new Session("scenario", 1, Map("foo" -> "FOO", "bar" -> "BAR"))
			val expression = Expression[String]("${foo} ${bar}")
			expression(session) must beEqualTo(Success("FOO BAR"))
		}

		"return expected result when used with a static index" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			val expression = Expression[String]("foo${bar(1)}")
			expression(session) must beEqualTo(Success("fooBAR2"))
		}
	}

	"'index' function in Expression" should {
		"return expected result when used with resolved index" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2"), "baz" -> 1))
			val expression = Expression[String]("{foo${bar(baz)}}")
			expression(session) must beEqualTo(Success("{fooBAR2}"))
		}

		"handle gracefully when used with static index and missing attribute" in {
			val session = new Session("scenario", 1, Map.empty)
			val expression = Expression[String]("foo${bar(1)}")
			expression(session) must beEqualTo(Failure(undefinedSessionAttributeMessage("bar")))
		}

		"handle gracefully when used with static index and empty attribute" in {
			val session = new Session("scenario", 1, Map("bar" -> Nil))
			val expression = Expression[String]("foo${bar(1)}")
			expression(session) must beEqualTo(Failure(undefinedSeqIndexMessage("bar", 1)))
		}

		"handle gracefully when used with static index and missing index" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1")))
			val expression = Expression[String]("foo${bar(1)}")
			expression(session) must beEqualTo(Failure(undefinedSeqIndexMessage("bar", 1)))
		}

		"handle gracefully when used with missing resolved index attribute" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			val expression = Expression[String]("{foo${bar(baz)}}")
			expression(session) must beEqualTo(Failure(undefinedSessionAttributeMessage("baz")))
		}
	}

	"'size' function in Expression" should {

		"return correct size for non empty seq" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			val expression = Expression[Int]("${bar.size}")
			expression(session) must beEqualTo(Success(2))
		}

		"return correct size for empty seq" in {
			val session = new Session("scenario", 1, Map("bar" -> List()))
			val expression = Expression[Int]("${bar.size}")
			expression(session) must beEqualTo(Success(0))
		}

		"return 0 size for missing attribute" in {
			val session = new Session("scenario", 1, Map())
			val expression = Expression[Int]("${bar.size}")
			expression(session) must beEqualTo(Failure(undefinedSessionAttributeMessage("bar")))
		}
	}

	"Malformed Expression" should {

		"be handled correctly when an attribute name is missing" in {
			val el = "foo${}bar"
			Expression[String](el) must throwA[ELMissingAttributeName]
		}

		"be handled correctly when there is a nested attribute definition" in {
			val el = "${foo${bar}}"
			Expression[String](el) must throwA[ELNestedAttributeDefinition]
		}
	}
}