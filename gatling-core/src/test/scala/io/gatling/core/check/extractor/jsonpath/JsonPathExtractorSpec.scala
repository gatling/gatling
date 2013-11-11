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

import scala.io.Codec.UTF8

import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.session.el.EL
import io.gatling.core.session.noopStringExpression
import io.gatling.core.test.ValidationSpecification
import io.gatling.core.util.IOHelper.withCloseable

@RunWith(classOf[JUnitRunner])
class JsonPathExtractorSpec extends ValidationSpecification {

	val noopSession = new Session("foo", "bar")

	def prepared(file: String) = {
		GatlingConfiguration.setUp()
		withCloseable(getClass.getResourceAsStream(file)) { is =>
			val string = IOUtils.toString(is, UTF8.charSet)
			JsonPathExtractor.parse(string)
		}
	}

	"count" should {
		
		def count(path: String, file: String) = new CountJsonPathExtractor(path.el)(noopSession, prepared(file))

		"return expected result with anywhere expression" in {
			count("$..author", "/test.json") must succeedWith(Some(4))
		}

		"return expected result with array expression" in {
			count("$.store.book[2].author", "/test.json") must succeedWith(Some(1))
		}
	}

	"extractSingle" should {
		
		def extractSingle(path: String, occurrence: Int, file: String) = new SingleJsonPathExtractor[String](path.el, occurrence).apply(noopSession, prepared(file))

		"return expected result with anywhere expression and rank 0" in {
			extractSingle("$..author", 0, "/test.json") must succeedWith(Some("Nigel Rees"))
		}

		"return expected result with anywhere expression and rank 1" in {
			extractSingle("$..author", 1, "/test.json") must succeedWith(Some("Evelyn Waugh"))
		}

		"return expected result with array expression" in {
			extractSingle("$.store.book[2].author", 0, "/test.json") must succeedWith(Some("Herman Melville"))
		}

		"return expected None with array expression" in {
			extractSingle("$.store.book[2].author", 1, "/test.json") must succeedWith(None)
		}

		"return expected result with last function expression" in {
			extractSingle("$.store.book[-1].title", 0, "/test.json") must succeedWith(Some("The Lord of the Rings"))
		}

		"not mess up if two nodes with the same name are placed in different locations" in {
			extractSingle("$.foo", 0, "/test.json") must succeedWith(Some("bar"))
		}

		"support bracket notation" in {
			extractSingle("$['@id']", 0, "/test.json") must succeedWith(Some("ID"))
		}

		"support element filter with object root" in {
			extractSingle("$..book[?(@.category=='reference')].author", 0, "/test.json") must succeedWith(Some("Nigel Rees"))
		}

		"support element filter with array root" in {
			extractSingle("$[?(@.id==19434)].foo", 0, "/test2.json") must succeedWith(Some("1"))
		}

		// $..[?()] is not a valid syntax
		//		"support element filter with wildcard" in {
		//			JsonPathExtractors.extractSingle(0)(prepared("/test2.json"), "$..[?(@.id==19434)].foo") must succeedWith(Some("1"))
		//		}

		"support multiple element filters" in {
			extractSingle("$[?(@.id==19434 && @.foo==1)].foo", 0, "/test2.json") must succeedWith(Some("1"))
		}
	}

	"extractMultiple" should {
		
		def extractMultiple(path: String, file: String) = new MultipleJsonPathExtractor[String](path.el).apply(noopSession, prepared(file))

		"return expected result with anywhere expression" in {
			extractMultiple("$..author", "/test.json") must succeedWith(Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
		}

		"return expected result with array expression" in {
			extractMultiple("$.store.book[2].author", "/test.json") must succeedWith(Some(List("Herman Melville")))
		}

		"support wildcard at first level" in {
			extractMultiple("$[*].id", "/test2.json") must succeedWith(Some(List("19434", "19435")))
		}

		"support wildcard at first level with multiple sublevels" in {
			extractMultiple("$..owner.id", "/test2.json") must succeedWith(Some(List("18957", "18957")))
		}

		"support wildcard at second level" in {
			extractMultiple("$..store..category", "/test.json") must succeedWith(Some(List("reference", "fiction", "fiction", "fiction")))
		}

		"support array slicing" in {
			extractMultiple("$.store.book[1:3].title", "/test.json") must succeedWith(Some(List("Sword of Honour", "Moby Dick")))
		}

		"support a step parameter in array slicing" in {
			extractMultiple("$.store.book[::-2].title", "/test.json") must succeedWith(Some(List("The Lord of the Rings", "Sword of Honour")))
		}
	}
}
