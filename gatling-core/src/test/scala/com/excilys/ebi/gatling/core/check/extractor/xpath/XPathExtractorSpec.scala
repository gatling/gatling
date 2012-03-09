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
import com.excilys.ebi.gatling.core.check.extractor.xpath.XPathExtractorSpec.extractor
import scala.io.Source

object XPathExtractorSpec {
	val document = getClass.getResourceAsStream("/test.xml")

	val extractor = new XPathExtractor(document)
}

@RunWith(classOf[JUnitRunner])
class XPathExtractorSpec extends Specification {

	val namespaces = List("foo" -> "http://foo/foo")

	"//author" should {

		"have count return 4" in {
			extractor.count(namespaces)("//author") must beEqualTo(Some(4))
		}

		"have extractMultiple return List(Nigel Rees, Evelyn Waugh, Herman Melville, J. R. R. Tolkien)" in {
			extractor.extractMultiple(namespaces)("//author") must beEqualTo(Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
		}

		"have extractOne(1) return Evelyn Waugh" in {
			extractor.extractOne(1, namespaces)("//author") must beEqualTo(Some("Evelyn Waugh"))
		}
	}

	"/test/store/book[3]/author" should {

		"have count return 1" in {
			extractor.count(namespaces)("/test/store/book[3]/author") must beEqualTo(Some(1))
		}

		"have extractMultiple return List(Herman Melville)" in {
			extractor.extractMultiple(namespaces)("/test/store/book[3]/author") must beEqualTo(Some(List("Herman Melville")))
		}

		"have extractOne(0) return None" in {
			extractor.extractOne(0, namespaces)("/test/store/book[3]/author") must beEqualTo(Some("Herman Melville"))
		}

		"have extractOne(1) return None" in {
			extractor.extractOne(1, namespaces)("/test/store/book[3]/author") must beEqualTo(None)
		}
	}

	"/test/store/book[@author='Nigel Rees']/title" should {

		"have extractOne(0) return Sayings of the Century" in {
			extractor.extractOne(0, namespaces)("/test/store/book[@att = 'foo']/title") must beEqualTo(Some("Sayings of the Century"))
		}
	}

	"//book[last()]/title" should {

		"have extractOne(0) return The Lord of the Rings" in {
			extractor.extractOne(0, namespaces)("//book[last()]/title") must beEqualTo(Some("The Lord of the Rings"))
		}
	}

	"//display-price" should {

		"have extractMultiple return List(8.95, 12.99, 8.99, 22.99, 19.95)" in {
			extractor.extractMultiple(namespaces)("//display-price") must beEqualTo(Some(List("8.95", "12.99", "8.99", "22.99", "19.95")))
		}
	}

	"//foo:bar" should {
		"have extractMultiple return List(fooBar)" in {
			extractor.extractMultiple(namespaces)("//foo:bar") must beEqualTo(Some(List("fooBar")))
		}
	}
}