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
import io.gatling.core.json.{ Jackson, Boon }
import io.gatling.core.test.ValidationValues
import io.gatling.core.util.IO._

class JsonPathExtractorSpec extends FlatSpec with Matchers with ValidationValues {

  GatlingConfiguration.setUp()

  def parseBoon(file: String): Object = withCloseable(getClass.getResourceAsStream(file)) { is =>
    val string = is.toString(StandardCharsets.UTF_8)
    Boon.parse(string)
  }

  def parseJackson(file: String): Object = withCloseable(getClass.getResourceAsStream(file)) { is =>
    val string = is.toString(StandardCharsets.UTF_8)
    Jackson.parse(string)
  }

  def testCount(path: String, file: String, expected: Int): Unit = {
    new CountJsonPathExtractor(path)(parseBoon(file)).succeeded shouldBe Some(expected)
    new CountJsonPathExtractor(path)(parseJackson(file)).succeeded shouldBe Some(expected)
  }
  def testSingle(path: String, occurrence: Int, file: String, expected: Option[String]): Unit = {
    new SingleJsonPathExtractor[String](path, occurrence).apply(parseBoon(file)).succeeded shouldBe expected
    new SingleJsonPathExtractor[String](path, occurrence).apply(parseJackson(file)).succeeded shouldBe expected
  }
  def testMultiple(path: String, file: String, expected: Option[List[String]]): Unit = {
    new MultipleJsonPathExtractor[String](path).apply(parseBoon(file)).succeeded shouldBe expected
    new MultipleJsonPathExtractor[String](path).apply(parseJackson(file)).succeeded shouldBe expected
  }

  "count" should "return expected result with anywhere expression" in {
    testCount("$..author", "/test.json", 4)
  }

  it should "return expected result with array expression" in {
    testCount("$.store.book[2].author", "/test.json", 1)
  }

  it should "return Some(0) when no results" in {
    testCount("$.bar", "/test.json", 0)
  }

  "extractSingle" should "return expected result with anywhere expression and rank 0" in {
    testSingle("$..author", 0, "/test.json", Some("Nigel Rees"))
  }

  it should "return expected result with anywhere expression and rank 1" in {
    testSingle("$..author", 1, "/test.json", Some("Evelyn Waugh"))
  }

  it should "return expected result with array expression" in {
    testSingle("$.store.book[2].author", 0, "/test.json", Some("Herman Melville"))
  }

  it should "return expected None with array expression" in {
    testSingle("$.store.book[2].author", 1, "/test.json", None)
  }

  it should "return expected result with last function expression" in {
    testSingle("$.store.book[-1].title", 0, "/test.json", Some("The Lord of the Rings"))
  }

  it should "not mess up if two nodes with the same name are placed in different locations" in {
    testSingle("$.foo", 0, "/test.json", Some("bar"))
  }

  it should "support bracket notation" in {
    testSingle("$['@id']", 0, "/test.json", Some("ID"))
  }

  it should "support element filter with object root" in {
    testSingle("$..book[?(@.category=='reference')].author", 0, "/test.json", Some("Nigel Rees"))
  }

  it should "support element filter with array root" in {
    testSingle("$[?(@.id==19434)].foo", 0, "/test2.json", Some("1"))
  }

  it should "support multiple element filters" in {
    testSingle("$[?(@.id==19434 && @.foo==1)].foo", 0, "/test2.json", Some("1"))
  }

  "extractMultiple" should "return expected result with anywhere expression" in {
    testMultiple("$..author", "/test.json", Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
  }

  it should "return expected result with array expression" in {
    testMultiple("$.store.book[2].author", "/test.json", Some(List("Herman Melville")))
  }

  it should "support wildcard at first level" in {
    testMultiple("$[*].id", "/test2.json", Some(List("19434", "19435")))
  }

  it should "support wildcard at first level with multiple sublevels" in {
    testMultiple("$..owner.id", "/test2.json", Some(List("18957", "18957")))
  }

  it should "support wildcard at second level" in {
    testMultiple("$..store..category", "/test.json", Some(List("reference", "fiction", "fiction", "fiction")))
  }

  it should "support array slicing" in {
    testMultiple("$.store.book[1:3].title", "/test.json", Some(List("Sword of Honour", "Moby Dick")))
  }

  it should "support a step parameter in array slicing" in {
    testMultiple("$.store.book[::-2].title", "/test.json", Some(List("The Lord of the Rings", "Sword of Honour")))
  }
}
