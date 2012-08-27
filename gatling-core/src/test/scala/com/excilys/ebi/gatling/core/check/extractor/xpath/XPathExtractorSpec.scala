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
package com.excilys.ebi.gatling.core.check.extractor.xpath

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class XPathExtractorSpec extends Specification {

	val namespaces = List("foo" -> "http://foo/foo")

	"count" should {

		"return expected result with anywhere expression" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.count(namespaces)("//author") must beEqualTo(Some(4))
		}

		"return expected result with array expression" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.count(namespaces)("/test/store/book[3]/author") must beEqualTo(Some(1))
		}
	}

	"extractOne" should {

		"return expected result with anywhere expression and rank 0" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.extractOne(0, namespaces)("//author") must beEqualTo(Some("Nigel Rees"))
		}

		"return expected result with anywhere expression and rank 1" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.extractOne(1, namespaces)("//author") must beEqualTo(Some("Evelyn Waugh"))
		}

		"return expected result with array expression" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.extractOne(0, namespaces)("/test/store/book[3]/author") must beEqualTo(Some("Herman Melville"))
		}

		"return expected None with array expression" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.extractOne(1, namespaces)("/test/store/book[3]/author") must beEqualTo(None)
		}

		"return expected result with attribute expression" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.extractOne(0, namespaces)("/test/store/book[@att = 'foo']/title") must beEqualTo(Some("Sayings of the Century"))
		}

		"return expected result with last function expression" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.extractOne(0, namespaces)("//book[last()]/title") must beEqualTo(Some("The Lord of the Rings"))
		}
	}

	"extractMultiple" should {

		"return expected result with anywhere expression" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.extractMultiple(namespaces)("//author") must beEqualTo(Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
		}

		"return expected result with array expression" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.extractMultiple(namespaces)("/test/store/book[3]/author") must beEqualTo(Some(List("Herman Melville")))
		}

		"return expected result with anywhere namespaced element" in {
			val extractor = XPathExtractor(getClass.getResourceAsStream("/test.xml"))
			extractor.extractMultiple(namespaces)("//foo:bar") must beEqualTo(Some(List("fooBar")))
		}
	}
}