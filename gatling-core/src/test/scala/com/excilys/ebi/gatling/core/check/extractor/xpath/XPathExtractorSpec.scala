/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.check.extractor.xpath

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.core.util.IOHelper

@RunWith(classOf[JUnitRunner])
class XPathExtractorSpec extends Specification {

	val namespaces = List("foo" -> "http://foo/foo")

	def extractor(file: String) = IOHelper.use(getClass.getResourceAsStream(file)) { stream =>
		XPathExtractor(stream)
	}

	"count" should {

		"return expected result with anywhere expression" in {
			extractor("/test.xml").count(namespaces)("//author") must beEqualTo(Some(4))
		}

		"return expected result with array expression" in {
			extractor("/test.xml").count(namespaces)("/test/store/book[3]/author") must beEqualTo(Some(1))
		}
	}

	"extractOne" should {

		"return expected result with anywhere expression and rank 0" in {
			extractor("/test.xml").extractOne(0, namespaces)("//author") must beEqualTo(Some("Nigel Rees"))
		}

		"support name()" in {
			extractor("/test.xml").extractOne(0, namespaces)("//*[name()='author']") must beEqualTo(Some("Nigel Rees"))
		}

		"return expected result with anywhere expression and rank 1" in {
			extractor("/test.xml").extractOne(1, namespaces)("//author") must beEqualTo(Some("Evelyn Waugh"))
		}

		"return expected result with array expression" in {
			extractor("/test.xml").extractOne(0, namespaces)("/test/store/book[3]/author") must beEqualTo(Some("Herman Melville"))
		}

		"return expected None with array expression" in {
			extractor("/test.xml").extractOne(1, namespaces)("/test/store/book[3]/author") must beEqualTo(None)
		}

		"return expected result with attribute expression" in {
			extractor("/test.xml").extractOne(0, namespaces)("/test/store/book[@att = 'foo']/title") must beEqualTo(Some("Sayings of the Century"))
		}

		"return expected result with last function expression" in {
			extractor("/test.xml").extractOne(0, namespaces)("//book[last()]/title") must beEqualTo(Some("The Lord of the Rings"))
		}

		"support default namespace" in {
			extractor("/test2.xml").extractOne(0, List("pre" -> "http://schemas.test.com/entityserver/runtime/1.0"))("//pre:name") must beEqualTo(Some("HR"))
		}
	}

	"extractMultiple" should {

		"return expected result with anywhere expression" in {
			extractor("/test.xml").extractMultiple(namespaces)("//author") must beEqualTo(Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
		}

		"return expected result with array expression" in {
			extractor("/test.xml").extractMultiple(namespaces)("/test/store/book[3]/author") must beEqualTo(Some(List("Herman Melville")))
		}

		"return expected result with anywhere namespaced element" in {
			extractor("/test.xml").extractMultiple(namespaces)("//foo:bar") must beEqualTo(Some(List("fooBar")))
		}
	}
}