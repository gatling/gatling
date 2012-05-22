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

import scala.io.Source

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.core.check.extractor.jsonpath.JsonExtractorSpec.extractor

object JsonExtractorSpec {
	val document = Source.fromInputStream(getClass.getResourceAsStream("/test.json")).mkString

	val extractor = new JsonPathExtractor(document)
}

@RunWith(classOf[JUnitRunner])
class JsonExtractorSpec extends Specification {

	"count" should {

		"return expected result with anywhere expression" in {
			extractor.count("//author") must beEqualTo(Some(4))
		}

		"return expected result with array expression" in {
			extractor.count("/store/book[3]/author") must beEqualTo(Some(1))
		}
	}

	"extractOne" should {

		"return expected result with anywhere expression and rank 0" in {
			extractor.extractOne(0)("//author") must beEqualTo(Some("Nigel Rees"))
		}

		"return expected result with anywhere expression and rank 1" in {
			extractor.extractOne(1)("//author") must beEqualTo(Some("Evelyn Waugh"))
		}

		"return expected result with array expression" in {
			extractor.extractOne(0)("/store/book[3]/author") must beEqualTo(Some("Herman Melville"))
		}

		"return expected None with array expression" in {
			extractor.extractOne(1)("/store/book[3]/author") must beEqualTo(None)
		}

		"return expected result with attribute expression" in {
			extractor.extractOne(0)("/store/book[@author = 'Nigel Rees']/title") must beEqualTo(Some("Sayings of the Century"))
		}

		"return expected result with last function expression" in {
			extractor.extractOne(0)("//book[last()]/title") must beEqualTo(Some("The Lord of the Rings"))
		}
	}

	"extractMultiple" should {

		"return expected result with anywhere expression" in {
			extractor.extractMultiple("//author") must beEqualTo(Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
		}

		"return expected result with array expression" in {
			extractor.extractMultiple("/store/book[3]/author") must beEqualTo(Some(List("Herman Melville")))
		}
	}
}