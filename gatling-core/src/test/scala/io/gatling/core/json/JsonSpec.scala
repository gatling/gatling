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

package io.gatling.core.json

import java.{ util => ju }
import java.nio.charset.StandardCharsets.UTF_8

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.Using

import io.gatling.BaseSpec
import io.gatling.commons.util.Io
import io.gatling.core.json.Json._

private final case class Foo(bar: String, baz: Int)

class JsonSpec extends BaseSpec {

  "stringify" should "be able to stringify strings" in {
    stringify("Foo", isRootObject = true) shouldBe "Foo"
  }

  it should "be able to stringify double-quoted strings" in {
    stringify("""Double quoted "Foo"""", isRootObject = true) shouldBe """Double quoted \"Foo\""""
  }

  it should "be able to stringify numbers" in {
    stringify(3.toByte, isRootObject = true) shouldBe "3"
    stringify(3.toShort, isRootObject = true) shouldBe "3"
    stringify(3, isRootObject = true) shouldBe "3"
    stringify(3L, isRootObject = true) shouldBe "3"
    stringify(4.5, isRootObject = true) shouldBe "4.5"
    stringify(4.5.toFloat, isRootObject = true) shouldBe "4.5"
  }

  it should "be able to stringify booleans" in {
    stringify(true, isRootObject = true) shouldBe "true"
    stringify(false, isRootObject = true) shouldBe "false"
  }

  it should "be able to stringify nulls" in {
    stringify(null, isRootObject = true) shouldBe "null"
  }

  it should "be able to stringify arrays" in {
    stringify(Array(1, 2, 3), isRootObject = true) shouldBe "[1,2,3]"
    stringify(Array("foo", "bar", "quz"), isRootObject = true) shouldBe """["foo","bar","quz"]"""
    stringify(Array(true, false, false), isRootObject = true) shouldBe "[true,false,false]"
  }

  it should "be able to stringify Scala collections" in {
    stringify(Seq(1, 2, 3), isRootObject = true) shouldBe "[1,2,3]"
    stringify(IndexedSeq(1, 2, 3), isRootObject = true) shouldBe "[1,2,3]"
    stringify(1 to 3, isRootObject = true) shouldBe "[1,2,3]"
    stringify(mutable.Buffer(1, 2, 3), isRootObject = true) shouldBe "[1,2,3]"
    stringify(mutable.Queue(1, 2, 3), isRootObject = true) shouldBe "[1,2,3]"
  }

  it should "be able to stringify Java collections" in {
    val list: ju.Collection[Int] = Seq(1, 2, 3).asJava
    stringify(list, isRootObject = true) shouldBe "[1,2,3]"
  }

  it should "be able to stringify nested collections" in {
    stringify(Seq(1, Seq(2, 3), 4), isRootObject = true) shouldBe "[1,[2,3],4]"
  }

  it should "be able to stringify Scala maps" in {
    stringify(Map(1 -> "foo", "bar" -> 4.5, "toto" -> Seq(1, 2)), isRootObject = true) shouldBe """{"1":"foo","bar":4.5,"toto":[1,2]}"""
  }

  it should "be able to stringify Scala maps with double quoted string values" in {
    stringify(
      Map(1 -> """Double quoted "Foo"""", "bar" -> 4.5, "toto" -> Seq(1, 2)),
      isRootObject = true
    ) shouldBe """{"1":"Double quoted \"Foo\"","bar":4.5,"toto":[1,2]}"""
  }

  it should "be able to stringify Java maps" in {
    val map: ju.Map[Any, Any] = Map(1 -> "foo", "bar" -> 4.5, "toto" -> Seq(1, 2)).asJava
    stringify(map, isRootObject = true) shouldBe """{"1":"foo","bar":4.5,"toto":[1,2]}"""
  }

  it should "not escape solidus" in {
    val url = "http://foo.com/bar/"
    stringify(url, isRootObject = true) shouldBe url
  }

  it should "not wrap null values with double quotes" in {
    val map: ju.Map[String, Any] = Map[String, Any]("name" -> "frodo", "note" -> null).asJava
    stringify(map, isRootObject = true) shouldBe """{"name":"frodo","note":null}"""
  }

  it should "support case classes" in {
    val foo = Foo("hello", 1)
    stringify(foo, isRootObject = true) shouldBe """{"bar":"hello","baz":1}"""
  }

  "asScala" should "deep convert into Scala structures" in {
    val input = Using.resource(Thread.currentThread().getContextClassLoader.getResourceAsStream("test.json")) { is =>
      new JsonParsers().parse(is, UTF_8)
    }

    asScala(input) shouldBe Seq(
      Map(
        "id" -> 19434,
        "foo" -> 1,
        "company" -> Map("id" -> 18971),
        "owner" -> Map("id" -> 18957),
        "process" -> Map("id" -> 18972)
      ),
      Map(
        "id" -> 19435,
        "foo" -> 2,
        "company" -> Map("id" -> 18972),
        "owner" -> Map("id" -> 18957),
        "process" -> Map("id" -> 18974)
      )
    )
  }
}
