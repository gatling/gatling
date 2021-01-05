/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check.jsonpath

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.core.json.JsonParsers

class JsonPathExtractorSpec extends BaseSpec with ValidationValues {

  private val jsonPaths = new JsonPaths(Long.MaxValue)
  private val jsonParsers = new JsonParsers

  def testCount(path: String, sample: JsonSample, expected: Int): Unit = {
    val extractor = JsonPathExtractors.count("jsonPath", path, jsonPaths)
    extractor(sample.jacksonAST(jsonParsers)).succeeded shouldBe Some(expected)
  }
  def testFind[T: JsonFilter](path: String, occurrence: Int, sample: JsonSample, expected: Option[T]): Unit = {
    val extractor = JsonPathExtractors.find[T]("jsonPath", path, occurrence, jsonPaths)
    extractor(sample.jacksonAST(jsonParsers)).succeeded shouldBe expected
  }
  def testFindAll[T: JsonFilter](path: String, sample: JsonSample, expected: Option[List[T]]): Unit = {
    val extractor = JsonPathExtractors.findAll[T]("jsonPath", path, jsonPaths)
    extractor(sample.jacksonAST(jsonParsers)).succeeded shouldBe expected
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

  "find" should "return expected result with anywhere expression and rank 0" in {
    testFind("$..author", 0, Json1, Some("Nigel Rees"))
  }

  it should "return expected result with anywhere expression and rank 1" in {
    testFind("$..author", 1, Json1, Some("Evelyn Waugh"))
  }

  it should "return expected result with array expression" in {
    testFind("$.store.book[2].author", 0, Json1, Some("Herman Melville"))
  }

  it should "return expected None with array expression" in {
    testFind[String]("$.store.book[2].author", 1, Json1, None)
  }

  it should "return expected result with last function expression" in {
    testFind("$.store.book[-1].title", 0, Json1, Some("The Lord of the Rings"))
  }

  it should "not mess up if two nodes with the same name are placed in different locations" in {
    testFind("$.foo", 0, Json1, Some("bar"))
  }

  it should "support bracket notation" in {
    testFind("$['@id']", 0, Json1, Some("ID"))
  }

  it should "support element filter with object root" in {
    testFind("$..book[?(@.category=='reference')].author", 0, Json1, Some("Nigel Rees"))
  }

  it should "support element filter with array root" in {
    testFind("$[?(@.id==19434)].foo", 0, Json2, Some("1"))
  }

  it should "support multiple element filters" in {
    testFind("$[?(@.id==19434 && @.foo==1)].foo", 0, Json2, Some("1"))
  }

  it should "support @" in {
    testFind("$.object[*]['@id']", 0, Json3, Some("3"))
  }

  it should "support null attribute value when expected type is Any" in {
    testFind[Any]("$.foo", 0, new JsonSample { val value = """{"foo": null}""" }, Some(null))
  }

  it should "support null attribute value when expected type is String" in {
    testFind[String]("$.foo", 0, new JsonSample { val value = """{"foo": null}""" }, Some(null))
  }

  it should "support null attribute value when expected type is Int" in {
    testFind[Int]("$.foo", 0, new JsonSample { val value = """{"foo": null}""" }, Some(null.asInstanceOf[Int]))
  }

  it should "support square braces in filter compared String" in {
    testFind("$.error[?(@.errorMessage=='my service message, actualError=Not Found [404]')].errorCode", 0, Json4, Some("87263"))
  }

  it should "not escape solidus" in {
    testFind("$.url", 0, new JsonSample { val value = """{ "url":"http://test-login.test.com/test/" }""" }, Some("http://test-login.test.com/test/"))
  }

  it should "support long values" in {
    testFind("$.number", 0, new JsonSample { val value = s"""{"number": ${Long.MaxValue}}""" }, Some(Long.MaxValue))
  }

  "findAll" should "return expected result with anywhere expression" in {
    testFindAll("$..author", Json1, Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
  }

  it should "return expected result with array expression" in {
    testFindAll("$.store.book[2].author", Json1, Some(List("Herman Melville")))
  }

  it should "support wildcard at first level" in {
    testFindAll("$[*].id", Json2, Some(List("19434", "19435")))
  }

  it should "support wildcard at first level with multiple sublevels" in {
    testFindAll("$..owner.id", Json2, Some(List("18957", "18957")))
  }

  it should "support wildcard at second level" in {
    testFindAll("$..store..category", Json1, Some(List("reference", "fiction", "fiction", "fiction")))
  }

  it should "support array slicing" in {
    testFindAll("$.store.book[1:3].title", Json1, Some(List("Sword of Honour", "Moby Dick")))
  }

  it should "support a step parameter in array slicing" in {
    testFindAll("$.store.book[::-2].title", Json1, Some(List("The Lord of the Rings", "Sword of Honour")))
  }
}
