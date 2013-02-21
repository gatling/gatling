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
package com.excilys.ebi.gatling.core.check.extractor.jsonpath

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.core.config.GatlingConfiguration

@RunWith(classOf[JUnitRunner])
class JsonPathExtractorSpec extends Specification {

	def extractor(file: String) = {
		GatlingConfiguration.setUp()
		new JsonPathExtractor(Some(getClass.getResourceAsStream(file)))
	}

	"count" should {

		"return expected result with anywhere expression" in {
			extractor("/test.json").count("//author") must beEqualTo(Some(4))
		}

		"return expected result with array expression" in {
			extractor("/test.json").count("/store/book[3]/author") must beEqualTo(Some(1))
		}
	}

	"extractOne" should {

		"return expected result with anywhere expression and rank 0" in {
			extractor("/test.json").extractOne(0)("//author") must beEqualTo(Some("Nigel Rees"))
		}

		"return expected result with anywhere expression and rank 1" in {
			extractor("/test.json").extractOne(1)("//author") must beEqualTo(Some("Evelyn Waugh"))
		}

		"return expected result with array expression" in {
			extractor("/test.json").extractOne(0)("/store/book[3]/author") must beEqualTo(Some("Herman Melville"))
		}

		"return expected None with array expression" in {
			extractor("/test.json").extractOne(1)("/store/book[3]/author") must beEqualTo(None)
		}

		"return expected result with attribute expression" in {
			extractor("/test.json").extractOne(0)("/store/book[@author = 'Nigel Rees']/title") must beEqualTo(Some("Sayings of the Century"))
		}

		"return expected result with last function expression" in {
			extractor("/test.json").extractOne(0)("//book[last()]/title") must beEqualTo(Some("The Lord of the Rings"))
		}

		"not mess up if two nodes with the same name are placed in different locations" in {
			extractor("/test.json").extractOne(0)("/foo") must beEqualTo(Some("bar"))
		}
	}

	"extractMultiple" should {

		"return expected result with anywhere expression" in {
			extractor("/test.json").extractMultiple("//author") must beEqualTo(Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
		}

		"return expected result with array expression" in {
			extractor("/test.json").extractMultiple("/store/book[3]/author") must beEqualTo(Some(List("Herman Melville")))
		}

		"support wildcard at first level" in {
			extractor("/test2.json").extractMultiple("/*/id") must beEqualTo(Some(List("19434", "19435")))
		}

		"support wildcard at first level with multiple sublevels" in {
			extractor("/test2.json").extractMultiple("/*/owner/id") must beEqualTo(Some(List("18957", "18957")))
		}

		"support wildcard at second level" in {
			extractor("/test.json").extractMultiple("/store/*/category") must beEqualTo(Some(List("reference", "fiction", "fiction", "fiction")))
		}
	}
}