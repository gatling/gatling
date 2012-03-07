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
package com.excilys.ebi.gatling.core.check.extractor.json
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import JsonExtractorSpec.extractor

object JsonExtractorSpec {
	// @formatter:off
	val document =
		"{ \"store\": {\n" +
			"    \"book\": [ \n" +
			"      { \"category\": \"reference\",\n" +
			"        \"author\": \"Nigel Rees\",\n" +
			"        \"title\": \"Sayings of the Century\",\n" +
			"        \"display-price\": 8.95\n" +
			"      },\n" +
			"      { \"category\": \"fiction\",\n" +
			"        \"author\": \"Evelyn Waugh\",\n" +
			"        \"title\": \"Sword of Honour\",\n" +
			"        \"display-price\": 12.99\n" +
			"      },\n" +
			"      { \"category\": \"fiction\",\n" +
			"        \"author\": \"Herman Melville\",\n" +
			"        \"title\": \"Moby Dick\",\n" +
			"        \"isbn\": \"0-553-21311-3\",\n" +
			"        \"display-price\": 8.99\n" +
			"      },\n" +
			"      { \"category\": \"fiction\",\n" +
			"        \"author\": \"J. R. R. Tolkien\",\n" +
			"        \"title\": \"The Lord of the Rings\",\n" +
			"        \"isbn\": \"0-395-19395-8\",\n" +
			"        \"display-price\": 22.99\n" +
			"      }\n" +
			"    ],\n" +
			"    \"bicycle\": {\n" +
			"      \"color\": \"red\",\n" +
			"      \"display-price\": 19.95,\n" +
			"      \"foo:bar\": \"fooBar\",\n" +
			"      \"dot.notation\": \"new\",\n" +
			"      \"dash-notation\": \"dashes\"\n" +
			"    }\n" +
			"  },\n" +
			"  \"foo\": \"bar\"\n" +
			"}"
	// @formatter:on

	val extractor = new JsonExtractor(document)
}

@RunWith(classOf[JUnitRunner])
class JsonExtractorSpec extends Specification {

	"//author" should {

		"have count return 4" in {
			extractor.count("//author") must beEqualTo(Some(4))
		}

		"have extractMultiple return List(Nigel Rees, Evelyn Waugh, Herman Melville, J. R. R. Tolkien)" in {
			extractor.extractMultiple("//author") must beEqualTo(Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
		}

		"have extractOne(1) return Evelyn Waugh" in {
			extractor.extractOne(1)("//author") must beEqualTo(Some("Evelyn Waugh"))
		}
	}

	"/store/book[3]/author" should {

		"have count return 1" in {
			extractor.count("/store/book[3]/author") must beEqualTo(Some(1))
		}

		"have extractMultiple return List(Herman Melville)" in {
			extractor.extractMultiple("/store/book[3]/author") must beEqualTo(Some(List("Herman Melville")))
		}

		"have extractOne(0) return None" in {
			extractor.extractOne(0)("/store/book[3]/author") must beEqualTo(Some("Herman Melville"))
		}

		"have extractOne(1) return None" in {
			extractor.extractOne(1)("/store/book[3]/author") must beEqualTo(None)
		}
	}

	"//display-price" should {

		"have extractMultiple return List(8.95, 12.99, 8.99, 22.99, 19.95)" in {
			extractor.extractMultiple("//display-price") must beEqualTo(Some(List("8.95", "12.99", "8.99", "22.99", "19.95")))
		}
	}
}