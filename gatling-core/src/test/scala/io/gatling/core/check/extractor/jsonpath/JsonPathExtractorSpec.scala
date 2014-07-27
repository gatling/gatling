/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.nio.charset.StandardCharsets

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.Boon
import io.gatling.core.test.ValidationValues
import io.gatling.core.util.IO._

class JsonPathExtractorSpec extends FlatSpec with Matchers with ValidationValues {

  GatlingConfiguration.setUp()

  def prepared(file: String): Object = withCloseable(getClass.getResourceAsStream(file)) { is =>
    val string = is.toString(StandardCharsets.UTF_8)
    Boon.parse(string)
  }

  def count(path: String, file: String) = new CountJsonPathExtractor(path)(prepared(file))
  def extractSingle(path: String, occurrence: Int, file: String) = new SingleJsonPathExtractor[String](path, occurrence).apply(prepared(file))
  def extractMultiple(path: String, file: String) = new MultipleJsonPathExtractor[String](path).apply(prepared(file))

  "count" should "return expected result with anywhere expression" in {
    count("$..author", "/test.json").succeeded shouldBe Some(4)
  }

  it should "return expected result with array expression" in {
    count("$.store.book[2].author", "/test.json").succeeded shouldBe Some(1)
  }

  it should "return Some(0) when no results" in {
    count("$.bar", "/test.json").succeeded shouldBe Some(0)
  }

  "extractSingle" should "return expected result with anywhere expression and rank 0" in {
    extractSingle("$..author", 0, "/test.json").succeeded shouldBe Some("Nigel Rees")
  }

  it should "return expected result with anywhere expression and rank 1" in {
    extractSingle("$..author", 1, "/test.json").succeeded shouldBe Some("Evelyn Waugh")
  }

  it should "return expected result with array expression" in {
    extractSingle("$.store.book[2].author", 0, "/test.json").succeeded shouldBe Some("Herman Melville")
  }

  it should "return expected None with array expression" in {
    extractSingle("$.store.book[2].author", 1, "/test.json").succeeded shouldBe None
  }

  it should "return expected result with last function expression" in {
    extractSingle("$.store.book[-1].title", 0, "/test.json").succeeded shouldBe Some("The Lord of the Rings")
  }

  it should "not mess up if two nodes with the same name are placed in different locations" in {
    extractSingle("$.foo", 0, "/test.json").succeeded shouldBe Some("bar")
  }

  it should "support bracket notation" in {
    extractSingle("$['@id']", 0, "/test.json").succeeded shouldBe Some("ID")
  }

  it should "support element filter with object root" in {
    extractSingle("$..book[?(@.category=='reference')].author", 0, "/test.json").succeeded shouldBe Some("Nigel Rees")
  }

  it should "support element filter with array root" in {
    extractSingle("$[?(@.id==19434)].foo", 0, "/test2.json").succeeded shouldBe Some("1")
  }

  // $..[?()] is not a valid syntax
  //		"support element filter with wildcard" in {
  //			extractSingle("$..[?(@.id==19434)].foo", 0, "/test2.json").succeeded shouldBe(Some("1"))
  //		}

  it should "support multiple element filters" in {
    extractSingle("$[?(@.id==19434 && @.foo==1)].foo", 0, "/test2.json").succeeded shouldBe Some("1")
  }

  it should "not try to be too smart and try funky stuff to parse dates" in {

    val string = """{
  "email":"bobby.tables@example.com"
}"""

    new SingleJsonPathExtractor[String]("$.email", 0).apply(Boon.parse(string)).succeeded shouldBe Some("bobby.tables@example.com")
  }

  "extractMultiple" should "return expected result with anywhere expression" in {
    extractMultiple("$..author", "/test.json").succeeded shouldBe Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien"))
  }

  it should "return expected result with array expression" in {
    extractMultiple("$.store.book[2].author", "/test.json").succeeded shouldBe Some(List("Herman Melville"))
  }

  it should "support wildcard at first level" in {
    extractMultiple("$[*].id", "/test2.json").succeeded shouldBe Some(List("19434", "19435"))
  }

  it should "support wildcard at first level with multiple sublevels" in {
    extractMultiple("$..owner.id", "/test2.json").succeeded shouldBe Some(List("18957", "18957"))
  }

  it should "support wildcard at second level" in {
    extractMultiple("$..store..category", "/test.json").succeeded shouldBe Some(List("reference", "fiction", "fiction", "fiction"))
  }

  it should "support array slicing" in {
    extractMultiple("$.store.book[1:3].title", "/test.json").succeeded shouldBe Some(List("Sword of Honour", "Moby Dick"))
  }

  it should "support a step parameter in array slicing" in {
    extractMultiple("$.store.book[::-2].title", "/test.json").succeeded shouldBe Some(List("The Lord of the Rings", "Sword of Honour"))
  }
}
