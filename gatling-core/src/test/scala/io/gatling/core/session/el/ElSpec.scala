/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.core.session.el

import java.{ util => ju }
import java.util.UUID

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.commons.validation.Success
import io.gatling.core.EmptySession
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.el

private final case class Foo(bar: String)

private final case class Bar(baz: Baz)

private final case class Baz(qix: String)

final case class Primitives(
    boolean: Boolean,
    byte: Byte,
    short: Short,
    int: Int,
    long: Long,
    float: Float,
    double: Double,
    char: Char
)

class ElSpec extends BaseSpec with ValidationValues with EmptySession {
  private implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private def newSession(attributes: Map[String, Any]) =
    emptySession.copy(attributes = attributes)

  "Static String" should "return itself" in {
    val session = newSession(Map.empty)
    val expression = "bar".el[String]
    expression(session).succeeded shouldBe "bar"
  }

  it should "return empty when empty" in {
    val session = newSession(Map.empty)
    val expression = "".el[String]
    expression(session).succeeded shouldBe ""
  }

  "One monovalued Expression" should "return expected result when the variable is the whole string" in {
    val session = newSession(Map("bar" -> "BAR"))
    val expression = "#{bar}".el[String]
    expression(session).succeeded shouldBe "BAR"
  }

  it should "return expected result when the variable is at the end of the string" in {
    val session = newSession(Map("bar" -> "BAR"))
    val expression = "foo#{bar}".el[String]
    expression(session).succeeded shouldBe "fooBAR"
  }

  it should "return expected result when the variable is at the beginning of the string" in {
    val session = newSession(Map("bar" -> "BAR"))
    val expression = "#{bar}baz".el[String]
    expression(session).succeeded shouldBe "BARbaz"
  }

  it should "return expected result when the variable is in the middle of the string" in {
    val session = newSession(Map("bar" -> "BAR"))
    val expression = "foo#{bar}baz".el[String]
    expression(session).succeeded shouldBe "fooBARbaz"
  }

  it should "handle gracefully when an attribute is missing" in {
    val session = newSession(Map("foo" -> "FOO"))
    val expression = "foo#{bar}".el[String]
    expression(session).failed shouldBe ElMessages.undefinedSessionAttribute("bar").message
  }

  it should "properly handle JsonPath expression" in {
    val session = newSession(Map("foo" -> "FOO"))
    val expression = "$.foo.bar".el[String]
    expression(session).succeeded shouldBe "$.foo.bar"
  }

  it should "properly handle JsonPath expression template" in {
    val session = newSession(Map("foo" -> "FOO"))
    val expression = "$.#{foo}.bar".el[String]
    expression(session).succeeded shouldBe "$.FOO.bar"
  }

  it should "properly handle multiline JSON template" in {
    val session = newSession(Map("foo" -> "FOO"))
    val expression = """{
        "foo": #{foo},
        "bar": 1
      }""".el[String]
    expression(session).succeeded shouldBe """{
        "foo": FOO,
        "bar": 1
      }"""
  }

  "Multivalued Expression" should "return expected result with 2 monovalued expressions" in {
    val session = newSession(Map("foo" -> "FOO", "bar" -> "BAR"))
    val expression = "#{foo} #{bar}".el[String]
    expression(session).succeeded shouldBe "FOO BAR"
  }

  it should "return expected result when used with a static index" in {
    val session = newSession(Map("bar" -> List("BAR1", "BAR2")))
    val expression = "foo#{bar(1)}".el[String]
    expression(session).succeeded shouldBe "fooBAR2"
  }

  "index access" should "return n-th element of a Seq when n is a static number" in {
    val session = newSession(Map("bar" -> List("BAR1", "BAR2")))
    val expression = "#{bar(0)}".el[String]
    expression(session).succeeded shouldBe "BAR1"
  }

  it should "return n-th element of a Seq when n is a variable" in {
    val session = newSession(Map("bar" -> List("BAR1", "BAR2"), "baz" -> 1))
    val expression = "{foo#{bar(baz)}}".el[String]
    expression(session).succeeded shouldBe "{fooBAR2}"
  }

  it should "return n-th element of an Array" in {
    val session = newSession(Map("arr" -> Array(1, 2)))
    val expression = "#{arr(0)}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return n-th element of JList" in {
    val lst = new ju.LinkedList[Int]
    lst.add(1)
    lst.add(2)
    val session = newSession(Map("lst" -> lst))
    val expression = "#{lst(0)}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return (length + n)-th element of a Seq when n is a negative number" in {
    val session = newSession(Map("bar" -> List("BAR1", "BAR2")))
    val expression = "#{bar(-1)}".el[String]
    expression(session).succeeded shouldBe "BAR2"
  }

  it should "return (length + n)-th element of an Array when n is a negative number" in {
    val session = newSession(Map("arr" -> Array(1, 2)))
    val expression = "#{arr(1)}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return (length + n)-th element of a JList when n is a negative number" in {
    val lst = new ju.LinkedList[Int]
    lst.add(1)
    lst.add(2)
    val session = newSession(Map("lst" -> lst))
    val expression = "#{lst(-1)}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "handle gracefully when index in an Array is out of range" in {
    val session = newSession(Map("arr" -> Array(1, 2)))
    val expression = "#{arr(2)}".el[Int]
    expression(session).failed shouldBe ElMessages.undefinedSeqIndex("arr", 2).message
  }

  it should "handle gracefully when index in an JList is out of range" in {
    val lst = new ju.LinkedList[Int]
    lst.add(1)
    lst.add(2)
    val session = newSession(Map("lst" -> lst))
    val expression = "#{lst(2)}".el[Int]
    expression(session).failed shouldBe ElMessages.undefinedSeqIndex("lst", 2).message
  }

  it should "handle gracefully when used with static index and missing attribute" in {
    val session = newSession(Map.empty)
    val expression = "foo#{bar(1)}".el[String]
    expression(session).failed shouldBe ElMessages.undefinedSessionAttribute("bar").message
  }

  it should "handle gracefully when used with static index and empty attribute" in {
    val session = newSession(Map("bar" -> Nil))
    val expression = "foo#{bar(1)}".el[String]
    expression(session).failed shouldBe ElMessages.undefinedSeqIndex("bar", 1).message
  }

  it should "handle gracefully when used with static index and missing index" in {
    val session = newSession(Map("bar" -> List("BAR1")))
    val expression = "foo#{bar(1)}".el[String]
    expression(session).failed shouldBe ElMessages.undefinedSeqIndex("bar", 1).message
  }

  it should "handle gracefully when used with missing resolved index attribute" in {
    val session = newSession(Map("bar" -> List("BAR1", "BAR2")))
    val expression = "{foo#{bar(baz)}}".el[String]
    expression(session).failed shouldBe ElMessages.undefinedSessionAttribute("baz").message
  }

  it should "handle gracefully value of unsupported type" in {
    val session = newSession(Map("i" -> 1))
    val expression = "#{i(0)}".el[Int]
    expression(session).failed shouldBe ElMessages.indexAccessNotSupported(1, "i").message
  }

  it should "support tuples" in {
    val session = newSession(Map("tuple" -> ("foo", "bar")))
    val expression = "#{tuple(0)}".el[String]
    expression(session).succeeded shouldBe "foo"
  }

  "'size' function in Expression" should "return correct size for non empty seq" in {
    val session = newSession(Map("bar" -> List("BAR1", "BAR2")))
    val expression = "#{bar.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for empty seq" in {
    val session = newSession(Map("bar" -> List()))
    val expression = "#{bar.size()}".el[Int]
    expression(session).succeeded shouldBe 0
  }

  it should "return 0 size for missing attribute" in {
    val session = newSession(Map.empty)
    val expression = "#{bar.size()}".el[Int]
    expression(session).failed shouldBe ElMessages.undefinedSessionAttribute("bar").message
  }

  it should "return correct size for a non empty Array" in {
    val session = newSession(Map("arr" -> Array(1, 2)))
    val expression = "#{arr.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for a non empty JList" in {
    val lst = new java.util.LinkedList[Int]
    lst.add(1)
    lst.add(2)
    val session = newSession(Map("lst" -> lst))
    val expression = "#{lst.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for a non empty map" in {
    val session = newSession(Map("map" -> Map("key1" -> "val1", "key2" -> "val2")))
    val expression = "#{map.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for a non empty JSet" in {
    val set = new java.util.HashSet[Int]
    set.add(1)
    set.add(2)
    val session = newSession(Map("set" -> set))
    val expression = "#{set.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for a non empty JMap" in {
    val map = new ju.HashMap[Int, Int]
    map.put(1, 1)
    map.put(2, 2)
    val session = newSession(Map("map" -> map))
    val expression = "#{map.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "handle gracefully unsupported type" in {
    val session = newSession(Map("i" -> 10))
    val expression = "#{i.size()}".el[Int]
    expression(session).failed shouldBe ElMessages.sizeNotSupported(10, "i").message
  }

  "'random' function in Expression" should "return one of elements of List" in {
    val elements = List("BAR1", "BAR2")
    val session = newSession(Map("bar" -> elements))
    val expression = "#{bar.random()}".el[String]
    expression(session).succeeded should (be("BAR1") or be("BAR2"))
  }

  it should "return one of elements of JList" in {
    val list = new ju.ArrayList[Int]
    list.add(1)
    list.add(2)
    val session = newSession(Map("lst" -> list))
    val expression = "#{lst.random()}".el[Int]
    expression(session).succeeded should (be(1) or be(2))
  }

  it should "return one of elements of Array" in {
    val session = newSession(Map("arr" -> Array(1, 2)))
    val expression = "#{arr.random()}".el[Int]
    expression(session).succeeded should (be(1) or be(2))
  }

  it should "handle unsupported type" in {
    val session = newSession(Map("i" -> 10))
    val expression = "#{i.random()}".el[Int]
    expression(session).failed shouldBe el.ElMessages.randomNotSupported(10, "i").message
  }

  "'exists' function in Expression" should "validate that a value is in the session" in {
    val session = newSession(Map("key1" -> "val1"))
    val expression = "#{key1.exists()}".el[Boolean]
    expression(session).succeeded shouldBe true
  }

  it should "validate that a value is not in the session" in {
    val session = newSession(Map.empty)
    val expression = "#{key1.exists()}".el[Boolean]
    expression(session).succeeded shouldBe false
  }

  "access map in Expression" should "return value by key" in {
    val map = Map("key1" -> "val1", "key2" -> "val2")
    val session = newSession(Map("map" -> map))
    val expression = "#{map.key1}".el[String]
    expression(session).succeeded shouldBe "val1"
  }

  it should "return value by key from JMap" in {
    val map = new ju.HashMap[String, Int]
    map.put("key1", 1)
    map.put("key2", 2)
    val session = newSession(Map("map" -> map))
    val expression = "#{map.key1}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "handle missing map correctly" in {
    val session = newSession(Map.empty)
    val expression = "#{map.key1}".el[String]
    expression(session).failed shouldBe ElMessages.undefinedSessionAttribute("map").message
  }

  it should "handle nested map access" in {
    val session = newSession(Map("map" -> Map("key" -> Map("subKey" -> "val"))))
    val expression = "#{map.key.subKey}".el[String]
    expression(session).succeeded shouldBe "val"
  }

  it should "handle missing value correctly" in {
    val map = Map("key" -> "val")
    val session = newSession(Map("map" -> map))
    val expression = "#{map.nonexisting}".el[String]
    expression(session).failed shouldBe ElMessages.undefinedMapKey("map", "nonexisting").message
  }

  it should "handle missing value in JMap correctly" in {
    val map = new ju.HashMap[String, Int]
    map.put("key1", 1)
    val session = newSession(Map("map" -> map))
    val expression = "#{map.nonexisting}".el[Int]
    expression(session).failed shouldBe ElMessages.undefinedMapKey("map", "nonexisting").message
  }

  it should "handle wrong type correctly" in {
    val session = newSession(Map("i" -> 1))
    val expression = "#{i.key}".el[Int]
    expression(session).failed shouldBe ElMessages.accessByKeyNotSupported(1, "i").message
  }

  it should "support case classes String attribute" in {
    val session = newSession(Map("foo" -> Foo("hello")))
    val expression = "#{foo.bar}".el[String]
    expression(session).succeeded shouldBe "hello"
  }

  it should "extract case classes attributes with primitive types" in {
    val session = emptySession.set("foo", Primitives(boolean = true, 1, 1, 1, 1L, 1.1.toFloat, 1.1, 'a'))
    "#{foo.boolean}".el[Boolean].apply(session).succeeded shouldBe true
    "#{foo.byte}".el[Byte].apply(session).succeeded shouldBe 1
    "#{foo.short}".el[Short].apply(session).succeeded shouldBe 1
    "#{foo.int}".el[Int].apply(session).succeeded shouldBe 1
    "#{foo.long}".el[Long].apply(session).succeeded shouldBe 1L
    "#{foo.float}".el[Float].apply(session).succeeded shouldBe 1.1.toFloat
    "#{foo.double}".el[Double].apply(session).succeeded shouldBe 1.1
    "#{foo.char}".el[Char].apply(session).succeeded shouldBe 'a'
  }

  it should "support POJO String attributes" in {
    val pojo = new MyPojo
    pojo.setMyString("hello")

    val session = newSession(Map("foo" -> pojo))
    val expression = "#{foo.myString}".el[String]
    expression(session).succeeded shouldBe "hello"
  }

  it should "support POJO boolean attributes" in {
    val pojo = new MyPojo
    pojo.setMyBoolean(true)

    val session = newSession(Map("foo" -> pojo))
    val expression = "#{foo.myBoolean}".el[Boolean]
    expression(session).succeeded shouldBe true
  }

  it should "fail on POJO null attributes" in {
    val pojo = new MyPojo

    val session = newSession(Map("foo" -> pojo))
    val expression = "#{foo.myString}".el[String]
    expression(session).failed shouldBe "Value is null"
  }

  it should "handle wrong key" in {
    val session = newSession(Map("foo" -> Foo("hello")))
    val expression = "#{foo.qix}".el[String]
    expression(session).failed shouldBe ElMessages.undefinedMapKey("foo", "qix").message
  }

  it should "handle deeply nested case classes" in {
    val session = newSession(Map("bar" -> Bar(Baz("fux"))))
    val expression = "#{bar.baz.qix}".el[String]
    expression(session).succeeded shouldBe "fux"
  }

  "multiple level access" should "return an element of a sub-list" in {
    val lst = List(List(1, 2), List(3, 4))
    val session = newSession(Map("lst" -> lst))
    val expression = "#{lst(0)(0)}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return key of a map in a list" in {
    val lst = List(Map("key" -> "val"))
    val session = newSession(Map("lst" -> lst))
    val expression = "#{lst(0).key}".el[String]
    expression(session).succeeded shouldBe "val"
  }

  it should "return an element of a list from a map" in {
    val map = Map("lst" -> List(1, 2))
    val session = newSession(Map("map" -> map))
    val expression = "#{map.lst(0)}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return a value from a sub-map" in {
    val map = Map("subMap" -> Map("key" -> "val"))
    val session = newSession(Map("map" -> map))
    val expression = "#{map.subMap.key}".el[String]
    expression(session).succeeded shouldBe "val"
  }

  it should "return a value from a random list" in {
    val lst = List(List(1, 2), List(3, 4))
    val session = newSession(Map("lst" -> lst))
    val expression = "#{lst.random()(0)}".el[Int]
    expression(session).succeeded should (be(1) or be(3))
  }

  it should "return size of a sub-list" in {
    val lst = List(List(1, 2), List(3, 4, 5))
    val session = newSession(Map("lst" -> lst))
    val expression = "#{lst(1).size()}".el[Int]
    expression(session).succeeded shouldBe 3
  }

  it should "name of a value is correct" in {
    val lst = List(Map("key" -> "value"))
    val session = newSession(Map("lst" -> lst))
    val expression = "#{lst(0).key.nonexisting}".el[Int]
    expression(session).failed shouldBe ElMessages.accessByKeyNotSupported("value", "lst(0).key").message
  }

  "tuples access" should "return size of Tuple2" in {
    val session = newSession(Map("tuple" -> Tuple2(1, 1)))
    val expression = "#{tuple.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return size of Tuple3" in {
    val session = newSession(Map("tuple" -> Tuple3(1, 1, 1)))
    val expression = "#{tuple.size()}".el[Int]
    expression(session).succeeded shouldBe 3
  }

  it should "return first element of a Tuple" in {
    val session = newSession(Map("tuple" -> Tuple3(1, 2, 3)))
    val expression = "#{tuple._1}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return last element of a Tuple" in {
    val session = newSession(Map("tuple" -> Tuple3(1, 2, 3)))
    val expression = "#{tuple._3}".el[Int]
    expression(session).succeeded shouldBe 3
  }

  it should "return a random element of a Tuple" in {
    val session = newSession(Map("tuple" -> Tuple3(1, 2, 3)))
    val expression = "#{tuple.random()}".el[Int]
    expression(session).succeeded should (be(1) or be(2) or be(3))
  }

  it should "return element with zero index in a Tuple" in {
    val tuple = Tuple3(1, 2, 3)
    val session = newSession(Map("tuple" -> tuple))
    val expression = "#{tuple._0}".el[Int]
    expression(session).failed shouldBe ElMessages.outOfRangeAccess("tuple", tuple, 0).message
  }

  it should "return element of range" in {
    val tuple = Tuple3(1, 2, 3)
    val session = newSession(Map("tuple" -> tuple))
    val expression = "#{tuple._4}".el[Int]
    expression(session).failed shouldBe ElMessages.outOfRangeAccess("tuple", tuple, 4).message
  }

  it should "handle correctly if object do not support tuple access" in {
    val int = 5
    val session = newSession(Map("i" -> int))
    val expression = "#{i._3}".el[Int]
    expression(session).failed shouldBe ElMessages.tupleAccessNotSupported("i", int).message
  }

  "pairs access" should "return size of a Pair" in {
    val session = newSession(Map("pair" -> (1 -> 2)))
    val expression = "#{pair.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return first element of a pair" in {
    val session = newSession(Map("pair" -> (1 -> 2)))
    val expression = "#{pair._1}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return second element of a pair" in {
    val session = newSession(Map("pair" -> (1 -> 2)))
    val expression = "#{pair._2}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return random element of a pair" in {
    val session = newSession(Map("pair" -> (1 -> 2)))
    val expression = "#{pair.random()}".el[Int]
    expression(session).succeeded should (be(1) or be(2))
  }

  it should "return zero element of a pair" in {
    val pair = 1 -> 2
    val session = newSession(Map("pair" -> pair))
    val expression = "#{pair._0}".el[Int]
    expression(session).failed shouldBe ElMessages.outOfRangeAccess("pair", pair, 0).message
  }

  it should "return out of range element of a pair" in {
    val pair = 1 -> 2
    val session = newSession(Map("pair" -> pair))
    val expression = "#{pair._3}".el[Int]
    expression(session).failed shouldBe ElMessages.outOfRangeAccess("pair", pair, 3).message
  }

  "Malformed Expression" should "be handled correctly when an attribute name is missing" in {
    a[ElParserException] should be thrownBy "foo#{}bar".el[String]
  }

  it should "be handled when ${ is not closed" in {
    a[ElParserException] should be thrownBy "#{foo".el[String]
  }

  "'isUndefined' function in Expression" should "validate that a value is not in the session" in {
    val session = newSession(Map.empty)
    val expression = "#{key1.isUndefined()}".el[Boolean]
    expression(session).succeeded shouldBe true
  }

  it should "validate that a value is in the session" in {
    val session = newSession(Map("key1" -> "value1"))
    val expression = "#{key1.isUndefined()}".el[Boolean]
    expression(session).succeeded shouldBe false
  }

  it should "be handled correctly when there is a nested attribute definition with string before nested attribute" in {
    a[ElParserException] should be thrownBy "#{foo${bar}}".el[String]
  }

  it should "be handled correctly when there is a nested attribute definition with string after nested attribute" in {
    a[ElParserException] should be thrownBy "#{#{bar}foo}".el[String]
  }

  it should "be handled correctly when there is a nested attribute definition with string before and after nested attribute" in {
    a[ElParserException] should be thrownBy "#{foo#{bar}foo}".el[String]
  }

  it should "be handled correctly when there is a nested attribute definition" in {
    a[ElParserException] should be thrownBy "#{#{bar}}".el[String]
  }

  it should "be handled correctly when there are several nested attributes" in {
    a[ElParserException] should be thrownBy "#{#{bar}${bar}}".el[String]
  }

  "jsonStringify" should "support String value" in {
    val session = newSession(Map("value" -> "VALUE"))
    val expression = """"name": #{value.jsonStringify()}""".el[String]
    expression(session).succeeded shouldBe """"name": "VALUE""""
  }

  it should "support number value" in {
    val session = newSession(Map("value" -> 5.0))
    val expression = """"name": #{value.jsonStringify()}""".el[String]
    expression(session).succeeded shouldBe """"name": 5.0"""
  }

  it should "support null value" in {
    val session = newSession(Map("value" -> null))
    val expression = """"name": #{value.jsonStringify()}""".el[String]
    expression(session).succeeded shouldBe """"name": null"""
  }

  it should "support arrays" in {
    val session = newSession(Map("value" -> Array("a", 1)))
    val expression = "#{value.jsonStringify()}".el[String]
    expression(session).succeeded shouldBe """["a",1]"""
  }

  it should "support maps/objects" in {
    val session = newSession(Map("value" -> Map("foo" -> "bar", "baz" -> 1)))
    val expression = "#{value.jsonStringify()}".el[String]
    expression(session).succeeded shouldBe """{"foo":"bar","baz":1}"""
  }

  it should "support case classes" in {
    val session = newSession(Map("bar" -> Bar(Baz("fux"))))
    val expression = "#{bar.jsonStringify()}".el[String]
    expression(session).succeeded shouldBe """{"baz":{"qix":"fux"}}"""
  }

  it should "support POJOs" in {
    val pojo = new MyPojo
    pojo.setMyString("hello")
    val session = newSession(Map("pojo" -> pojo))
    val expression = "#{pojo.jsonStringify()}".el[String]
    expression(session).succeeded shouldBe """{"myString":"hello","myBoolean":false}"""
  }

  it should "support key access" in {
    val json = Map("bar" -> Map("baz" -> "qix"))
    val session = newSession(Map("foo" -> json))
    val expression = "#{foo.bar.jsonStringify()}".el[String]
    expression(session).succeeded shouldBe """{"baz":"qix"}"""
  }

  it should "return the original failure when failing" in {
    val session = newSession(Map("foo" -> "bar"))
    val failedKeyAccessExpression = "#{foo.bar}".el[String]
    val failedJsonStringifyExpression = "#{foo.bar.jsonStringify()}".el[String]
    failedJsonStringifyExpression(session).failed shouldBe failedKeyAccessExpression(session).failed
  }

  "currentTimeMillis" should "generate a long" in {
    val session = newSession(Map("foo" -> "bar"))
    val currentTimeMillisExpression = "#{currentTimeMillis()}".el[Long]
    currentTimeMillisExpression(session).succeeded shouldBe a[Long]
  }

  "currentDate" should "generate a String" in {
    val currentDateExpression = "#{currentDate(yyyy-MM-dd HH:mm:ss)}".el[String]
    currentDateExpression(emptySession).succeeded.length shouldBe 19
  }

  it should "support patterns with a dot" in {
    val currentDateExpression = """#{currentDate("YYYY-MM-dd'T'HH:mm:ss.SSSMs'Z'")}""".el[String]
    currentDateExpression(emptySession) shouldBe a[Success[_]]
  }

  "htmlUnescape" should "escape HTML entities" in {
    val expression = "#{foo.htmlUnescape()}".el[String]
    val session = newSession(Map("foo" -> "foo &eacute; bar"))
    expression(session).succeeded shouldBe "foo é bar"
  }

  "Escaping" should "turn \\#{ into #{" in {
    val session = newSession(Map("foo" -> "FOO"))
    val expression = "\\#{foo}".el[String]
    expression(session).succeeded shouldBe "#{foo}"
  }

  "randomUuid" should "generate uuid" in {
    val randomUuid = "#{randomUuid()}".el[String]
    UUID.fromString(randomUuid(emptySession).succeeded)
  }

  "randomSecureUuid" should "generate uuid" in {
    val randomSecureUuid = "#{randomSecureUuid()}".el[String]
    UUID.fromString(randomSecureUuid(emptySession).succeeded)
  }

  "randomInt" should "generate random Int full range" in {
    val randomInt = "#{randomInt()}".el[Int]
    randomInt(emptySession).succeeded should (be >= Int.MinValue and be <= Int.MaxValue)
  }

  "randomIntRange" should "generate random Int within range (left inclusive and right exclusive)" in {
    val randomInt = "#{randomInt(0,10)}".el[Int]
    val actual = Set.fill(1000)(randomInt(emptySession).succeeded)
    val expected = (0 until 10).toSet
    actual should contain theSameElementsAs expected
  }

  "randomIntRange" should "generate random Int with negative numbers" in {
    val randomInt = "#{randomInt(-10,-5)}".el[Int]
    randomInt(emptySession).succeeded should (be >= -10 and be < -5)
  }

  "randomIntRange" should "throw exception with 'max' less than 'min'" in {
    an[ElParserException] should be thrownBy "#{randomInt(20,1)}".el[Int]
  }

  "randomLong" should "generate random Long with full range" in {
    val randomLong = "#{randomLong()}".el[Long]
    randomLong(emptySession).succeeded should (be >= Long.MinValue and be <= Long.MaxValue)
  }

  "randomLongRange" should "generate random Long with range (left inclusive and right exclusive)" in {
    val randomLong = "#{randomLong(2147483648,2147483658)}".el[Long]
    val actual = Set.fill(1000)(randomLong(emptySession).succeeded)
    val expected = (Int.MaxValue + 1L until Int.MaxValue + 11L).toSet
    actual should contain theSameElementsAs expected
  }

  "randomLongRange" should "generate random Long with negative numbers" in {
    val randomInt = "#{randomLong(-2147483658,-2147483648)}".el[Long]
    randomInt(emptySession).succeeded should (be >= -2147483658L and be < -2147483648L)
  }

  "randomLongRange" should "throw exception with 'max' less than 'min'" in {
    an[ElParserException] should be thrownBy "#{randomLong(2147483658,2147483648)}".el[Long]
  }

  "randomDoubleRange" should "throw an ELParse exception when first parameter is digit.digit.digit" in {
    a[ElParserException] should be thrownBy "#{randomDouble(0.42.1,42.42)}".el[Double]
  }

  it should "throw an ELParse exception when second parameter is not digit.digit.digit" in {
    a[ElParserException] should be thrownBy "#{randomDouble(0.42,42.42.42)}".el[Double]
  }

  it should "throw an ELParse exception when parameter has .digit" in {
    a[ElParserException] should be thrownBy "#{randomDouble(.42,1.42)}".el[Double]
  }

  it should "throw an ELParse exception when parameter has digit." in {
    a[ElParserException] should be thrownBy "#{randomDouble(0.42,1.)}".el[Double]
  }

  it should "throw an ELParse exception when second parameter is a string" in {
    a[ElParserException] should be thrownBy "#{randomDouble(0.42,a)}".el[Double]
  }

  it should "throw an ELParse exception when first parameter is a character" in {
    a[ElParserException] should be thrownBy "#{randomDouble(abc,42.42)}".el[Double]
  }

  it should "generate random Double" in {
    val randomDouble = "#{randomDouble(-0.42,42.42)}".el[Double]
    all(List.fill(420)(randomDouble(emptySession).succeeded)) should (be >= -.42 and be < 42.42)
  }

  "randomDoubleRangeDigits" should "generate random Double with limited fractional digits" in {
    val randomDouble = "#{randomDouble(1111.11,2222.22,4)}".el[Double]
    val foo = List.fill(420)(randomDouble(emptySession).succeeded)
    all(foo.map(s => s.toString.split("\\.")(1).length)) should be <= 9
  }

  it should "generate random Double with 0 fractional digits sets to 1 fractional digit with digits.0" in {
    val randomDouble = "#{randomDouble(-3.14,3.14,0)}".el[Double]
    all(List.fill(420)(randomDouble(emptySession).succeeded.toString.split("\\.")(1)).map(s => (s.length, s))) should be <= (1, "0")
  }

  it should "throw exception when max <= min" in {
    a[ElParserException] should be thrownBy "#{randomDouble(42.42,42.42)}".el[Double]
  }

  "randomDoubleRangeDigits" should "throw exception num of digits is less than 0" in {
    a[ElParserException] should be thrownBy "#{randomDouble(4.242,42.42,-42)}".el[Double]
  }

  it should "throw exception when max <= min given a valid fractionalDigits" in {
    a[ElParserException] should be thrownBy "#{randomDouble(42.42,42.42,42)}".el[Double]
  }

  "randomAlphanumeric" should "generate random alphanum by length" in {
    val randomAlphanumeric = "#{randomAlphanumeric(10)}".el[String]
    randomAlphanumeric(emptySession).succeeded.length shouldBe 10
  }
}
