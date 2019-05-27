/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import java.util.{ Collection => JCollection, Map => JMap }

import scala.collection.mutable
import scala.collection.JavaConverters._

import io.gatling.BaseSpec
import io.gatling.commons.util.Io
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.Json._

class JsonSpec extends BaseSpec {

  "stringify" should "be able to stringify strings" in {
    stringify("Foo") shouldBe "Foo"
  }

  it should "be able to stringify double-quoted strings" in {
    stringify("""Double quoted "Foo"""") shouldBe """Double quoted \"Foo\""""
  }

  it should "be able to stringify numbers" in {
    stringify(3.toByte) shouldBe "3"
    stringify(3.toShort) shouldBe "3"
    stringify(3) shouldBe "3"
    stringify(3L) shouldBe "3"
    stringify(4.5) shouldBe "4.5"
    stringify(4.5.toFloat) shouldBe "4.5"
  }

  it should "be able to stringify booleans" in {
    stringify(true) shouldBe "true"
    stringify(false) shouldBe "false"
  }

  it should "be able to stringify nulls" in {
    stringify(null) shouldBe "null"
  }

  it should "be able to stringify arrays" in {
    stringify(Array(1, 2, 3)) shouldBe "[1,2,3]"
    stringify(Array("foo", "bar", "quz")) shouldBe """["foo","bar","quz"]"""
    stringify(Array(true, false, false)) shouldBe "[true,false,false]"
  }

  it should "be able to stringify Scala collections" in {
    stringify(Seq(1, 2, 3)) shouldBe "[1,2,3]"
    stringify(IndexedSeq(1, 2, 3)) shouldBe "[1,2,3]"
    stringify(1 to 3) shouldBe "[1,2,3]"
    stringify(mutable.Buffer(1, 2, 3)) shouldBe "[1,2,3]"
    stringify(mutable.Queue(1, 2, 3)) shouldBe "[1,2,3]"
  }

  it should "be able to stringify Java collections" in {
    val list: JCollection[Int] = Seq(1, 2, 3).asJava
    stringify(list) shouldBe "[1,2,3]"
  }

  it should "be able to stringify nested collections" in {
    stringify(Seq(1, Seq(2, 3), 4)) shouldBe "[1,[2,3],4]"
  }

  it should "be able to stringify Scala maps" in {
    stringify(Map(1 -> "foo", "bar" -> 4.5, "toto" -> Seq(1, 2))) shouldBe """{"1":"foo","bar":4.5,"toto":[1,2]}"""
  }

  it should "be able to stringify Scala maps with double quoted string values" in {
    stringify(Map(1 -> """Double quoted "Foo"""", "bar" -> 4.5, "toto" -> Seq(1, 2))) shouldBe """{"1":"Double quoted \"Foo\"","bar":4.5,"toto":[1,2]}"""
  }

  it should "be able to stringify Java maps" in {
    val map: JMap[Any, Any] = Map(1 -> "foo", "bar" -> 4.5, "toto" -> Seq(1, 2)).asJava
    stringify(map) shouldBe """{"1":"foo","bar":4.5,"toto":[1,2]}"""
  }

  it should "not escape solidus" in {
    val url = "http://foo.com/bar/"
    stringify(url) shouldBe url
  }

  "asScala" should "deep convert into Scala structures" in {
    implicit val config = GatlingConfiguration.loadForTest()
    val input = Io.withCloseable(Thread.currentThread().getContextClassLoader.getResourceAsStream("test.json")) { is =>
      JsonParsers().parse(is)
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
