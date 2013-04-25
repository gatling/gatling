/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.session

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import io.gatling.core.test.ValidationSpecification

@RunWith(classOf[JUnitRunner])
class ELSpec extends ValidationSpecification {

	"One monovalued Expression" should {

		"return expected result when the variable is the whole string" in {
			val session = Session("scenario", 1, Map("bar" -> "BAR"))
			val expression = EL.compile[String]("${bar}")
			expression(session) must succeedWith("BAR")
		}

		"return expected result when the variable is at the end of the string" in {
			val session = Session("scenario", 1, Map("bar" -> "BAR"))
			val expression = EL.compile[String]("foo${bar}")
			expression(session) must succeedWith("fooBAR")
		}

		"return expected result when the variable is at the beginning of the string" in {
			val session = Session("scenario", 1, Map("bar" -> "BAR"))
			val expression = EL.compile[String]("${bar}baz")
			expression(session) must succeedWith("BARbaz")
		}

		"return expected result when the variable is in the middle of the string" in {
			val session = Session("scenario", 1, Map("bar" -> "BAR"))
			val expression = EL.compile[String]("foo${bar}baz")
			expression(session) must succeedWith("fooBARbaz")
		}

		"handle gracefully when an attribute is missing" in {
			val session = Session("scenario", 1, Map("foo" -> "FOO"))
			val expression = EL.compile[String]("foo${bar}")
			expression(session) must failWith(undefinedSessionAttributeMessage("bar"))
		}
	}

	"Multivalued Expression" should {

		"return expected result with 2 monovalued expressions" in {
			val session = Session("scenario", 1, Map("foo" -> "FOO", "bar" -> "BAR"))
			val expression = EL.compile[String]("${foo} ${bar}")
			expression(session) must succeedWith("FOO BAR")
		}

		"return expected result when used with a static index" in {
			val session = Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			val expression = EL.compile[String]("foo${bar(1)}")
			expression(session) must succeedWith("fooBAR2")
		}
	}

	"'index' function in Expression" should {
		"return expected result when used with resolved index" in {
			val session = Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2"), "baz" -> 1))
			val expression = EL.compile[String]("{foo${bar(baz)}}")
			expression(session) must succeedWith("{fooBAR2}")
		}

		"handle gracefully when used with static index and missing attribute" in {
			val session = Session("scenario", 1, Map.empty)
			val expression = EL.compile[String]("foo${bar(1)}")
			expression(session) must failWith(undefinedSessionAttributeMessage("bar"))
		}

		"handle gracefully when used with static index and empty attribute" in {
			val session = Session("scenario", 1, Map("bar" -> Nil))
			val expression = EL.compile[String]("foo${bar(1)}")
			expression(session) must failWith(undefinedSeqIndexMessage("bar", 1))
		}

		"handle gracefully when used with static index and missing index" in {
			val session = Session("scenario", 1, Map("bar" -> List("BAR1")))
			val expression = EL.compile[String]("foo${bar(1)}")
			expression(session) must failWith(undefinedSeqIndexMessage("bar", 1))
		}

		"handle gracefully when used with missing resolved index attribute" in {
			val session = Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			val expression = EL.compile[String]("{foo${bar(baz)}}")
			expression(session) must failWith(undefinedSessionAttributeMessage("baz"))
		}
	}

	"'size' function in Expression" should {

		"return correct size for non empty seq" in {
			val session = Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			val expression = EL.compile[Int]("${bar.size}")
			expression(session) must succeedWith(2)
		}

		"return correct size for empty seq" in {
			val session = Session("scenario", 1, Map("bar" -> List()))
			val expression = EL.compile[Int]("${bar.size}")
			expression(session) must succeedWith(0)
		}

		"return 0 size for missing attribute" in {
			val session = Session("scenario", 1)
			val expression = EL.compile[Int]("${bar.size}")
			expression(session) must failWith(undefinedSessionAttributeMessage("bar"))
		}
	}

	"Malformed Expression" should {

		"be handled correctly when an attribute name is missing" in {
			EL.compile[String]("foo${}bar") must throwA[ELMissingAttributeName]
		}

		"be handled correctly when there is a nested attribute definition" in {
			EL.compile[String]("${foo${bar}}") must throwA[ELNestedAttributeDefinition]
		}
	}
}