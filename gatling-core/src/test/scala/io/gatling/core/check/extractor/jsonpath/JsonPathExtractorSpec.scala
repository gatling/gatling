/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.check.extractor.jsonpath

import io.gatling.{ ValidationValues, BaseSpec }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers

class JsonPathExtractorSpec extends BaseSpec with ValidationValues {

  implicit val configuration = GatlingConfiguration.loadForTest()
  implicit val jsonPaths = new JsonPaths
  implicit val jsonParsers = JsonParsers()
  val extractorFactory = new JsonPathExtractorFactory
  import extractorFactory._

  def testCount(path: String, sample: JsonSample, expected: Int): Unit = {
    val extractor = newCountExtractor(path)
    extractor(sample.boonAST).succeeded shouldBe Some(expected)
    extractor(sample.jacksonAST).succeeded shouldBe Some(expected)
  }
  def testSingle[T](path: String, occurrence: Int, sample: JsonSample, expected: Option[T]): Unit = {
    val extractor = newSingleExtractor[String](path, occurrence)
    extractor(sample.boonAST).succeeded shouldBe expected
    extractor.apply(sample.jacksonAST).succeeded shouldBe expected
  }
  def testMultiple[T](path: String, sample: JsonSample, expected: Option[List[T]]): Unit = {
    val extractor = newMultipleExtractor[String](path)
    extractor(sample.boonAST).succeeded shouldBe expected
    extractor(sample.jacksonAST).succeeded shouldBe expected
  }

  "count" should "return expected result with anywhere expression" in {
    testCount("$..author", Json1, 4)
  }

  it should "return expected result with array expression" in {
    testCount("$.store.book[2].author", Json1, 1)
  }

  it should "return Some(0) when no results" in {
    testCount("$.bar", Json1, 0)
  }

  "extractSingle" should "return expected result with anywhere expression and rank 0" in {
    testSingle("$..author", 0, Json1, Some("Nigel Rees"))
  }

  it should "return expected result with anywhere expression and rank 1" in {
    testSingle("$..author", 1, Json1, Some("Evelyn Waugh"))
  }

  it should "return expected result with array expression" in {
    testSingle("$.store.book[2].author", 0, Json1, Some("Herman Melville"))
  }

  it should "return expected None with array expression" in {
    testSingle("$.store.book[2].author", 1, Json1, None)
  }

  it should "return expected result with last function expression" in {
    testSingle("$.store.book[-1].title", 0, Json1, Some("The Lord of the Rings"))
  }

  it should "not mess up if two nodes with the same name are placed in different locations" in {
    testSingle("$.foo", 0, Json1, Some("bar"))
  }

  it should "support bracket notation" in {
    testSingle("$['@id']", 0, Json1, Some("ID"))
  }

  it should "support element filter with object root" in {
    testSingle("$..book[?(@.category=='reference')].author", 0, Json1, Some("Nigel Rees"))
  }

  it should "support element filter with array root" in {
    testSingle("$[?(@.id==19434)].foo", 0, Json2, Some("1"))
  }

  it should "support multiple element filters" in {
    testSingle("$[?(@.id==19434 && @.foo==1)].foo", 0, Json2, Some("1"))
  }

  it should "support @" in {
    testSingle("$.object[*]['@id']", 0, Json3, Some("3"))
  }

  it should "support null attribute value when expected type is Any" in {
    testSingle("$.foo", 0, new JsonSample { val value = """{"foo": null}""" }, Some(null))
  }

  it should "support null attribute value when expected type is String" in {
    testSingle[String]("$.foo", 0, new JsonSample { val value = """{"foo": null}""" }, Some(null))
  }

  it should "support null attribute value when expected type is Int" in {
    testSingle[Int]("$.foo", 0, new JsonSample { val value = """{"foo": null}""" }, Some(null.asInstanceOf[Int]))
  }

  "extractMultiple" should "return expected result with anywhere expression" in {
    testMultiple("$..author", Json1, Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
  }

  it should "return expected result with array expression" in {
    testMultiple("$.store.book[2].author", Json1, Some(List("Herman Melville")))
  }

  it should "support wildcard at first level" in {
    testMultiple("$[*].id", Json2, Some(List("19434", "19435")))
  }

  it should "support wildcard at first level with multiple sublevels" in {
    testMultiple("$..owner.id", Json2, Some(List("18957", "18957")))
  }

  it should "support wildcard at second level" in {
    testMultiple("$..store..category", Json1, Some(List("reference", "fiction", "fiction", "fiction")))
  }

  it should "support array slicing" in {
    testMultiple("$.store.book[1:3].title", Json1, Some(List("Sword of Honour", "Moby Dick")))
  }

  it should "support a step parameter in array slicing" in {
    testMultiple("$.store.book[::-2].title", Json1, Some(List("The Lord of the Rings", "Sword of Honour")))
  }
}
