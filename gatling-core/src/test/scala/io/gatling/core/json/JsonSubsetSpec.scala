/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.core.json

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.commons.validation._
import io.gatling.core.check.jmespath.JmesPaths
import io.gatling.core.check.jsonpath.JsonPaths
import io.gatling.core.json.JsonSubset.Segment.{ AllElements, Field, Index }

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class JsonSubsetSpec extends AnyFlatSpecLike with Matchers {
  private val jsonParsers = new JsonParsers
  private val jsonPaths = new JsonPaths(100)
  private val jmesPaths = new JmesPaths(100)

  private def subset(segments: JsonSubset.Segment*): Option[JsonSubset] = JsonSubset(segments.toList)

  private def stream(json: String) = new ByteArrayInputStream(json.getBytes(UTF_8))

  private def partial(json: String, subset: JsonSubset) = jsonParsers.safeParsePartial(stream(json), subset)

  "JsonPaths.subset" should "support paths following fields, non-negative indexes and all the elements of arrays" in {
    jsonPaths.subset("$.meta.code") shouldBe subset(Field("meta"), Field("code"))
    jsonPaths.subset("$.data[0].id") shouldBe subset(Field("data"), Index(0), Field("id"))
    jsonPaths.subset("$['foo-bar'].code") shouldBe subset(Field("foo-bar"), Field("code"))
    jsonPaths.subset("$[2]") shouldBe subset(Index(2))
    jsonPaths.subset("$.data[*].id") shouldBe subset(Field("data"), AllElements, Field("id"))
    jsonPaths.subset("$[*].tags[*]") shouldBe subset(AllElements, Field("tags"), AllElements)
  }

  it should "not support paths that may read anything else" in {
    for (path <- Seq("$", "$..id", "$.data[*]..id", "$.data[-1]", "$.data[0,1]", "$.data[0:2]", "$.data[?(@.id == 1)]", "$.*", "$['a','b']", "invalid[")) {
      withClue(path)(jsonPaths.subset(path) shouldBe None)
    }
  }

  "JmesPaths.subset" should "support expressions following properties, non-negative indexes and list projections" in {
    // also pins the shape of jmespath-java's AST, which is read with reflection
    jmesPaths.subset("meta.code") shouldBe subset(Field("meta"), Field("code"))
    jmesPaths.subset("data[0].id") shouldBe subset(Field("data"), Index(0), Field("id"))
    jmesPaths.subset("\"foo-bar\".code") shouldBe subset(Field("foo-bar"), Field("code"))
    jmesPaths.subset(" meta . code ") shouldBe subset(Field("meta"), Field("code"))
    jmesPaths.subset("@.meta") shouldBe subset(Field("meta"))
    jmesPaths.subset("[1].id") shouldBe subset(Index(1), Field("id"))
    jmesPaths.subset("data[*].id") shouldBe subset(Field("data"), AllElements, Field("id"))
    jmesPaths.subset("data[*]") shouldBe subset(Field("data"), AllElements)
    jmesPaths.subset("data[*].tags[*].x") shouldBe subset(Field("data"), AllElements, Field("tags"), AllElements, Field("x"))
    jmesPaths.subset("a.data[*].b.c") shouldBe subset(Field("a"), Field("data"), AllElements, Field("b"), Field("c"))
    jmesPaths.subset("[*][0]") shouldBe subset(AllElements, Index(0))
  }

  it should "not support expressions that may read anything else" in {
    for (
      expression <- Seq(
        "@",
        "data[-1]",
        "data[0:2]",
        "data[0:2].id",
        "data[]",
        "data[].id",
        "*.id",
        "data[*] | [0]",
        "data[*].id | [0]",
        "a.data[*] | [0]",
        "(data[*]).id",
        "data[*].length(@)",
        "length(data)",
        "meta.{a: a}",
        "meta.[a, b]",
        "a || b",
        "data[?id == `1`]",
        "invalid["
      )
    ) {
      withClue(expression)(jmesPaths.subset(expression) shouldBe None)
    }
  }

  "JsonSubset.++" should "keep the whole value when a path is a prefix of another one" in {
    (subset(Field("meta")).get ++ subset(Field("meta"), Field("code")).get) shouldBe subset(Field("meta")).get
    (subset(Field("data"), AllElements).get ++ subset(Field("data"), Index(0), Field("id")).get) shouldBe subset(Field("data"), AllElements).get
  }

  private val documents = Seq(
    """{"meta":{"code":"OK","page":{"n":1}},"data":[{"id":1,"name":"a","tags":["x","y"]},{"id":2,"name":"b"},{"id":3}],"foo-bar":{"code":42}}""",
    """{"data":[{"id":1}],"meta":{"page":{"n":1},"code":"LAST"}}""",
    """{"meta":"scalar","data":{"0":"object, not array"}}""",
    """{"meta":{"code":null},"data":[]}""",
    """{"meta":{},"data":[[1,2],{"id":"nested"}]}""",
    """[{"id":1},{"id":2},{"id":3}]""",
    """{"meta":[{"code":"in array"}],"data":null}""",
    """{}""",
    """"root scalar"""",
    """{"data":[1,2,3,{"id":4}]}""",
    """{"data":[{"tags":[{"x":1},{"y":2},{"x":3}]},{"id":5,"tags":"scalar"},{"tags":[[{"x":4}]]}],"meta":{"code":"AFTER"}}"""
  )

  private val jsonPathExpressions =
    Seq(
      "$.meta",
      "$.meta.code",
      "$.meta.page.n",
      "$.data[0].id",
      "$.data[1].name",
      "$.data[2].id",
      "$.data[3].id",
      "$.data[0].tags",
      "$.data[5]",
      "$['foo-bar'].code",
      "$[1].id",
      "$.missing.code",
      "$.data[*]",
      "$.data[*].id",
      "$.data[*].tags[0]",
      "$.data[*].tags[*].x",
      "$[*].id",
      "$.meta[*].code"
    )

  private val jmesPathExpressions =
    Seq(
      "meta",
      "meta.code",
      "meta.page.n",
      "data[0].id",
      "data[1].name",
      "data[2].id",
      "data[3].id",
      "data[0].tags",
      "data[5]",
      "\"foo-bar\".code",
      "[1].id",
      "missing.code",
      "data[*]",
      "data[*].id",
      "data[*].tags[0]",
      "data[*].tags[*].x",
      "[*].id",
      "meta[*].code",
      "data[*][0]"
    )

  "the partial tree" should "give the same JsonPath results as the full tree" in {
    val union = jsonPathExpressions.flatMap(jsonPaths.subset).reduce(_ ++ _)
    for {
      document <- documents
      expression <- jsonPathExpressions
    } {
      val full = jsonParsers.parse(document)
      val expected = jsonPaths.extractAll[Any](full, expression).map(_.toList)
      for (s <- Seq(jsonPaths.subset(expression).get, union)) {
        withClue(s"$expression on $document: ") {
          partial(document, s).flatMap(jsonPaths.extractAll[Any](_, expression).map(_.toList)) shouldBe expected
        }
      }
    }
  }

  it should "give the same JMESPath results as the full tree" in {
    val union = jmesPathExpressions.flatMap(jmesPaths.subset).reduce(_ ++ _)
    for {
      document <- documents
      expression <- jmesPathExpressions
    } {
      val full = jsonParsers.parse(document)
      val expected = jmesPaths.extract[Any](full, expression)
      for (s <- Seq(jmesPaths.subset(expression).get, union)) {
        withClue(s"$expression on $document: ") {
          partial(document, s).flatMap(jmesPaths.extract[Any](_, expression)) shouldBe expected
        }
      }
    }
  }

  it should "stop parsing as soon as all the values have been read" in {
    val document = """{"meta":{"code":"OK"},"data":[this is not JSON"""
    jsonParsers.safeParse(document) shouldBe a[Failure]
    partial(document, jsonPaths.subset("$.meta.code").get).flatMap(jsonPaths.extractAll[String](_, "$.meta.code").map(_.toList)) shouldBe List("OK").success
  }

  it should "stop parsing at the end of an array whose elements are all needed" in {
    val document = """{"data":[{"id":1,"name":"a"},{"name":"b"},{"id":3}],"meta":{"code":"OK"},"padding":[this is not JSON"""
    val subset = jsonPaths.subset("$.data[*].id").get ++ jsonPaths.subset("$.meta.code").get
    partial(document, subset).flatMap(jsonPaths.extractAll[Int](_, "$.data[*].id").map(_.toList)) shouldBe List(1, 3).success
    partial(document, subset).flatMap(jsonPaths.extractAll[String](_, "$.meta.code").map(_.toList)) shouldBe List("OK").success
  }

  it should "not stop parsing before the end of an array whose elements are all needed" in {
    partial("""{"data":[{"id":1},{"id":2},this is not JSON""", jsonPaths.subset("$.data[*].id").get) shouldBe a[Failure]
  }

  it should "keep the first occurrence of a duplicated key" in {
    val document = """{"meta":{"code":"FIRST"},"meta":{"code":"LAST"}}"""
    partial(document, jsonPaths.subset("$.meta.code").get).flatMap(jsonPaths.extractAll[String](_, "$.meta.code").map(_.toList)) shouldBe List("FIRST").success
  }

  it should "fail on invalid JSON before all the values have been read" in {
    partial("""{"data":[this is not JSON""", jsonPaths.subset("$.meta.code").get) shouldBe a[Failure]
    partial("", jsonPaths.subset("$.meta.code").get) shouldBe a[Failure]
  }
}
