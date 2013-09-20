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
package io.gatling.core.check.extractor.jsonpath

import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.test.ValidationSpecification
import io.gatling.core.util.IOHelper.withCloseable

@RunWith(classOf[JUnitRunner])
class JaywayJsonPathExtractorsSpec extends ValidationSpecification {

	def prepared(file: String) = {
		GatlingConfiguration.setUp()
		withCloseable(getClass.getResourceAsStream(file)) { is =>
			val bytes = IOUtils.toByteArray(is)
			JaywayJsonPathExtractors.parse(bytes)
		}
	}

	"count" should {

		"return expected result with anywhere expression" in {
			JaywayJsonPathExtractors.count(prepared("/test.json"), "$..author") must succeedWith(Some(4))
		}

		"return expected result with array expression" in {
			JaywayJsonPathExtractors.count(prepared("/test.json"), "$.store.book[2].author") must succeedWith(Some(1))
		}
	}

	"extractOne" should {

		"return expected result with anywhere expression and rank 0" in {
			JaywayJsonPathExtractors.extractOne(0)(prepared("/test.json"), "$..author") must succeedWith(Some("Nigel Rees"))
		}

		"return expected result with anywhere expression and rank 1" in {
			JaywayJsonPathExtractors.extractOne(1)(prepared("/test.json"), "$..author") must succeedWith(Some("Evelyn Waugh"))
		}

		"return expected result with array expression" in {
			JaywayJsonPathExtractors.extractOne(0)(prepared("/test.json"), "$.store.book[2].author") must succeedWith(Some("Herman Melville"))
		}

		"return expected None with array expression" in {
			JaywayJsonPathExtractors.extractOne(1)(prepared("/test.json"), "$.store.book[2].author") must succeedWith(None)
		}

		"return expected result with last function expression" in {
			JaywayJsonPathExtractors.extractOne(0)(prepared("/test.json"), "$.store.book[(@.length - 1)].title") must succeedWith(Some("The Lord of the Rings"))
		}

		"not mess up if two nodes with the same name are placed in different locations" in {
			JaywayJsonPathExtractors.extractOne(0)(prepared("/test.json"), "$.foo") must succeedWith(Some("bar"))
		}

		"support bracket notation" in {
			JaywayJsonPathExtractors.extractOne(0)(prepared("/test.json"), "$.@id") must succeedWith(Some("ID"))
		}

		"support element filter with object root" in {
			JaywayJsonPathExtractors.extractOne(0)(prepared("/test.json"), "$..book[?(@.category=='reference')].author") must succeedWith(Some("Nigel Rees"))
		}

		"support element filter with array root" in {
			JaywayJsonPathExtractors.extractOne(0)(prepared("/test2.json"), "$.[?(@.id=='19434')].foo") must succeedWith(Some("1"))
		}

		"support element filter with wildcard" in {
			JaywayJsonPathExtractors.extractOne(0)(prepared("/test2.json"), "$..[?(@.id==19434)].foo") must succeedWith(Some("1"))
		}

		"support multiple element filters" in {
			JaywayJsonPathExtractors.extractOne(0)(prepared("/test2.json"), "$..[?(@.id==19434)][?(@.foo==1)].foo") must succeedWith(Some("1"))
		}
	}

	"extractMultiple" should {

		"return expected result with anywhere expression" in {
			JaywayJsonPathExtractors.extractMultiple(prepared("/test.json"), "$..author") must succeedWith(Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
		}

		"return expected result with array expression" in {
			JaywayJsonPathExtractors.extractMultiple(prepared("/test.json"), "$.store.book[2].author") must succeedWith(Some(List("Herman Melville")))
		}

		"support wildcard at first level" in {
			JaywayJsonPathExtractors.extractMultiple(prepared("/test2.json"), "$[*].id") must succeedWith(Some(List("19434", "19435")))
		}

		"support wildcard at first level with multiple sublevels" in {
			JaywayJsonPathExtractors.extractMultiple(prepared("/test2.json"), "$..owner.id") must succeedWith(Some(List("18957", "18957")))
		}

		"support wildcard at second level" in {
			JaywayJsonPathExtractors.extractMultiple(prepared("/test.json"), "$..store..category") must succeedWith(Some(List("reference", "fiction", "fiction", "fiction")))
		}
	}
}