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
package io.gatling.core.session.el

import java.util.{ ArrayList => JArrayList, HashMap => JHashMap, LinkedList => JLinkedList }

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.session.{ el, Session }
import io.gatling.core.test.ValidationValues

class ELSpec extends FlatSpec with Matchers with ValidationValues {

  "One monovalued Expression" should "return expected result when the variable is the whole string" in {
    val session = Session("scenario", "1", Map("bar" -> "BAR"))
    val expression = "${bar}".el[String]
    expression(session).succeeded shouldBe "BAR"
  }

  it should "return expected result when the variable is at the end of the string" in {
    val session = Session("scenario", "1", Map("bar" -> "BAR"))
    val expression = "foo${bar}".el[String]
    expression(session).succeeded shouldBe "fooBAR"
  }

  it should "return expected result when the variable is at the beginning of the string" in {
    val session = Session("scenario", "1", Map("bar" -> "BAR"))
    val expression = "${bar}baz".el[String]
    expression(session).succeeded shouldBe "BARbaz"
  }

  it should "return expected result when the variable is in the middle of the string" in {
    val session = Session("scenario", "1", Map("bar" -> "BAR"))
    val expression = "foo${bar}baz".el[String]
    expression(session).succeeded shouldBe "fooBARbaz"
  }

  it should "handle gracefully when an attribute is missing" in {
    val session = Session("scenario", "1", Map("foo" -> "FOO"))
    val expression = "foo${bar}".el[String]
    expression(session).failed shouldBe ELMessages.undefinedSessionAttribute("bar")
  }

  it should "properly handle JsonPath expression" in {
    val session = Session("scenario", "1", Map("foo" -> "FOO"))
    val expression = "$.foo.bar".el[String]
    expression(session).succeeded shouldBe "$.foo.bar"
  }

  it should "properly handle JsonPath expression template" in {
    val session = Session("scenario", "1", Map("foo" -> "FOO"))
    val expression = "$.${foo}.bar".el[String]
    expression(session).succeeded shouldBe "$.FOO.bar"
  }

  it should "properly handle multiline JSON template" in {
    val session = Session("scenario", "1", Map("foo" -> "FOO"))
    val expression = """{
        "foo": ${foo},
        "bar": 1
      }""".el[String]
    expression(session).succeeded shouldBe """{
        "foo": FOO,
        "bar": 1
      }"""
  }

  "Multivalued Expression" should "return expected result with 2 monovalued expressions" in {
    val session = Session("scenario", "1", Map("foo" -> "FOO", "bar" -> "BAR"))
    val expression = "${foo} ${bar}".el[String]
    expression(session).succeeded shouldBe "FOO BAR"
  }

  it should "return expected result when used with a static index" in {
    val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2")))
    val expression = "foo${bar(1)}".el[String]
    expression(session).succeeded shouldBe "fooBAR2"
  }

  "'index' function in Expression" should "return n-th element of a list in monovalued expression" in {
    val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2")))
    val expression = "${bar(0)}".el[String]
    expression(session).succeeded shouldBe "BAR1"
  }

  it should "return expected result when used with resolved index" in {
    val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2"), "baz" -> 1))
    val expression = "{foo${bar(baz)}}".el[String]
    expression(session).succeeded shouldBe "{fooBAR2}"
  }

  it should "return n-th element of an Array" in {
    val session = Session("scenario", "1", Map("arr" -> Array(1, 2)))
    val expression = "${arr(0)}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return n-th element of JList" in {
    val lst = new JLinkedList[Int]
    lst.add(1)
    lst.add(2)
    val session = Session("scenario", "1", Map("lst" -> lst))
    val expression = "${lst(0)}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "handle gracefully when index in an Array is out of range" in {
    val session = Session("scenario", "1", Map("arr" -> Array(1, 2)))
    val expression = "${arr(2)}".el[Int]
    expression(session).failed shouldBe ELMessages.undefinedSeqIndex("arr", 2)
  }

  it should "handle gracefully when index in an JList is out of range" in {
    val lst = new JLinkedList[Int]
    lst.add(1)
    lst.add(2)
    val session = Session("scenario", "1", Map("lst" -> lst))
    val expression = "${lst(2)}".el[Int]
    expression(session).failed shouldBe ELMessages.undefinedSeqIndex("lst", 2)
  }

  it should "handle gracefully when used with static index and missing attribute" in {
    val session = Session("scenario", "1", Map.empty)
    val expression = "foo${bar(1)}".el[String]
    expression(session).failed shouldBe ELMessages.undefinedSessionAttribute("bar")
  }

  it should "handle gracefully when used with static index and empty attribute" in {
    val session = Session("scenario", "1", Map("bar" -> Nil))
    val expression = "foo${bar(1)}".el[String]
    expression(session).failed shouldBe ELMessages.undefinedSeqIndex("bar", 1)
  }

  it should "handle gracefully when used with static index and missing index" in {
    val session = Session("scenario", "1", Map("bar" -> List("BAR1")))
    val expression = "foo${bar(1)}".el[String]
    expression(session).failed shouldBe ELMessages.undefinedSeqIndex("bar", 1)
  }

  it should "handle gracefully when used with missing resolved index attribute" in {
    val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2")))
    val expression = "{foo${bar(baz)}}".el[String]
    expression(session).failed shouldBe ELMessages.undefinedSessionAttribute("baz")
  }

  it should "handle gracefully value of unsupported type" in {
    val session = Session("scenario", "1", Map("i" -> 1))
    val expression = "${i(0)}".el[Int]
    expression(session).failed shouldBe ELMessages.indexAccessNotSupported(1, "i")
  }

  "'size' function in Expression" should "return correct size for non empty seq" in {
    val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2")))
    val expression = "${bar.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for empty seq" in {
    val session = Session("scenario", "1", Map("bar" -> List()))
    val expression = "${bar.size()}".el[Int]
    expression(session).succeeded shouldBe 0
  }

  it should "return 0 size for missing attribute" in {
    val session = Session("scenario", "1")
    val expression = "${bar.size()}".el[Int]
    expression(session).failed shouldBe ELMessages.undefinedSessionAttribute("bar")
  }

  it should "return correct size for a non empty Array" in {
    val session = Session("scenario", "1", Map("arr" -> Array(1, 2)))
    val expression = "${arr.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for a non empty JList" in {
    val lst = new java.util.LinkedList[Int]
    lst.add(1)
    lst.add(2)
    val session = Session("scenario", "1", Map("lst" -> lst))
    val expression = "${lst.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for a non empty map" in {
    val session = Session("scenario", "1", Map("map" -> Map("key1" -> "val1", "key2" -> "val2")))
    val expression = "${map.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for a non empty JSet" in {
    val set = new java.util.HashSet[Int]
    set.add(1)
    set.add(2)
    val session = Session("scenario", "1", Map("set" -> set))
    val expression = "${set.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return correct size for a non empty JMap" in {
    val map = new JHashMap[Int, Int]
    map.put(1, 1)
    map.put(2, 2)
    val session = Session("scenario", "1", Map("map" -> map))
    val expression = "${map.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "handle gracefully unsupported type" in {
    val session = Session("scenario", "1", Map("i" -> 10))
    val expression = "${i.size()}".el[Int]
    expression(session).failed shouldBe ELMessages.sizeNotSupported(10, "i")
  }

  "'random' function in Expression" should "return one of elements of List" in {
    val elements = List("BAR1", "BAR2")
    val session = Session("scenario", "1", Map("bar" -> elements))
    val expression = "${bar.random()}".el[String]
    expression(session).succeeded should (be("BAR1") or be("BAR2"))
  }

  it should "return one of elements of JList" in {
    val list = new JArrayList[Int]
    list.add(1)
    list.add(2)
    val session = Session("scenario", "1", Map("lst" -> list))
    val expression = "${lst.random()}".el[Int]
    expression(session).succeeded should (be(1) or be(2))
  }

  it should "return one of elements of Array" in {
    val session = Session("scenario", "1", Map("arr" -> Array(1, 2)))
    val expression = "${arr.random()}".el[Int]
    expression(session).succeeded should (be(1) or be(2))
  }

  it should "handle unsupported type" in {
    val session = Session("scenario", "1", Map("i" -> 10))
    val expression = "${i.random()}".el[Int]
    expression(session).failed shouldBe el.ELMessages.randomNotSupported(10, "i")
  }

  "access map in Expression" should "return value by key" in {
    val map = Map("key1" -> "val1", "key2" -> "val2")
    val session = Session("scenario", "1", Map("map" -> map))
    val expression = "${map.key1}".el[String]
    expression(session).succeeded shouldBe "val1"
  }

  it should "return value by key from JMap" in {
    val map = new JHashMap[String, Int]
    map.put("key1", 1)
    map.put("key2", 2)
    val session = Session("scenario", "1", Map("map" -> map))
    val expression = "${map.key1}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "handle missing map correctly" in {
    val session = Session("scenario", "1")
    val expression = "${map.key1}".el[String]
    expression(session).failed shouldBe ELMessages.undefinedSessionAttribute("map")
  }

  it should "handle nested map access" in {
    val map = Map("key" -> Map("subKey" -> "val"))
    val session = Session("scenario", "1", Map("map" -> map))
    val expression = "${map.key.subKey}".el[String]
    expression(session).succeeded shouldBe "val"
  }

  it should "handle missing value correctly" in {
    val map = Map("key" -> "val")
    val session = Session("scenario", "1", Map("map" -> map))
    val expression = "${map.nonexisting}".el[String]
    expression(session).failed shouldBe ELMessages.undefinedMapKey("map", "nonexisting")
  }

  it should "handle missing value in JMap correctly" in {
    val map = new JHashMap[String, Int]
    map.put("key1", 1)
    val session = Session("scenario", "1", Map("map" -> map))
    val expression = "${map.nonexisting}".el[Int]
    expression(session).failed shouldBe ELMessages.undefinedMapKey("map", "nonexisting")
  }

  it should "handle wrong type correctly" in {
    val session = Session("scenario", "1", Map("i" -> 1))
    val expression = "${i.key}".el[Int]
    expression(session).failed shouldBe ELMessages.accessByKeyNotSupported(1, "i")
  }

  "multiple level access" should "return an element of a sub-list" in {
    val lst = List(List(1, 2), List(3, 4))
    val session = Session("scenario", "1", Map("lst" -> lst))
    val expression = "${lst(0)(0)}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return key of a map in a list" in {
    val lst = List(Map("key" -> "val"))
    val session = Session("scenario", "1", Map("lst" -> lst))
    val expression = "${lst(0).key}".el[String]
    expression(session).succeeded shouldBe "val"
  }

  it should "return an element of a list from a map" in {
    val map = Map("lst" -> List(1, 2))
    val session = Session("scenario", "1", Map("map" -> map))
    val expression = "${map.lst(0)}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return a value from a sub-map" in {
    val map = Map("subMap" -> Map("key" -> "val"))
    val session = Session("scenario", "1", Map("map" -> map))
    val expression = "${map.subMap.key}".el[String]
    expression(session).succeeded shouldBe "val"
  }

  it should "return a value from a random list" in {
    val lst = List(List(1, 2), List(3, 4))
    val session = Session("scenario", "1", Map("lst" -> lst))
    val expression = "${lst.random()(0)}".el[Int]
    expression(session).succeeded should (be(1) or be(3))
  }

  it should "return size of a sub-list" in {
    val lst = List(List(1, 2), List(3, 4, 5))
    val session = Session("scenario", "1", Map("lst" -> lst))
    val expression = "${lst(1).size()}".el[Int]
    expression(session).succeeded shouldBe 3
  }

  it should "name of a value is correct" in {
    val lst = List(Map("key" -> "value"))
    val session = Session("scenario", "1", Map("lst" -> lst))
    val expression = "${lst(0).key.nonexisting}".el[Int]
    expression(session).failed shouldBe ELMessages.accessByKeyNotSupported("value", "lst(0).key")
  }

  "tuples access" should "return size of Tuple2" in {
    val session = Session("scenario", "1", Map("tuple" -> Tuple2(1, 1)))
    val expression = "${tuple.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return size of Tuple3" in {
    val session = Session("scenario", "1", Map("tuple" -> Tuple3(1, 1, 1)))
    val expression = "${tuple.size()}".el[Int]
    expression(session).succeeded shouldBe 3
  }

  it should "return first element of a Tuple" in {
    val session = Session("scenario", "1", Map("tuple" -> Tuple3(1, 2, 3)))
    val expression = "${tuple._1}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return last element of a Tuple" in {
    val session = Session("scenario", "1", Map("tuple" -> Tuple3(1, 2, 3)))
    val expression = "${tuple._3}".el[Int]
    expression(session).succeeded shouldBe 3
  }

  it should "return a random element of a Tuple" in {
    val session = Session("scenario", "1", Map("tuple" -> Tuple3(1, 2, 3)))
    val expression = "${tuple.random()}".el[Int]
    expression(session).succeeded should (be(1) or be(2) or be(3))
  }

  it should "return element with zero index in a Tuple" in {
    val tuple = Tuple3(1, 2, 3)
    val session = Session("scenario", "1", Map("tuple" -> tuple))
    val expression = "${tuple._0}".el[Int]
    expression(session).failed shouldBe ELMessages.outOfRangeAccess("tuple", tuple, 0)
  }

  it should "return element of range" in {
    val tuple = Tuple3(1, 2, 3)
    val session = Session("scenario", "1", Map("tuple" -> tuple))
    val expression = "${tuple._4}".el[Int]
    expression(session).failed shouldBe ELMessages.outOfRangeAccess("tuple", tuple, 4)
  }

  it should "handle correctly if object do not support tuple access" in {
    val int = 5
    val session = Session("scenario", "1", Map("i" -> int))
    val expression = "${i._3}".el[Int]
    expression(session).failed shouldBe ELMessages.tupleAccessNotSupported("i", int)
  }

  "pairs access" should "return size of a Pair" in {
    val session = Session("scenario", "1", Map("pair" -> (1 -> 2)))
    val expression = "${pair.size()}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return first element of a pair" in {
    val session = Session("scenario", "1", Map("pair" -> (1 -> 2)))
    val expression = "${pair._1}".el[Int]
    expression(session).succeeded shouldBe 1
  }

  it should "return second element of a pair" in {
    val session = Session("scenario", "1", Map("pair" -> (1 -> 2)))
    val expression = "${pair._2}".el[Int]
    expression(session).succeeded shouldBe 2
  }

  it should "return random element of a pair" in {
    val session = Session("scenario", "1", Map("pair" -> (1 -> 2)))
    val expression = "${pair.random()}".el[Int]
    expression(session).succeeded should (be(1) or be(2))
  }

  it should "return zero element of a pair" in {
    val pair = 1 -> 2
    val session = Session("scenario", "1", Map("pair" -> pair))
    val expression = "${pair._0}".el[Int]
    expression(session).failed shouldBe ELMessages.outOfRangeAccess("pair", pair, 0)
  }

  it should "return out of range element of a pair" in {
    val pair = 1 -> 2
    val session = Session("scenario", "1", Map("pair" -> pair))
    val expression = "${pair._3}".el[Int]
    expression(session).failed shouldBe ELMessages.outOfRangeAccess("pair", pair, 3)
  }

  "Malformed Expression" should "be handled correctly when an attribute name is missing" in {
    a[ELParserException] should be thrownBy "foo${}bar".el[String]
  }

  it should "be handled correctly when there is a nested attribute definition with string before nested attribute" in {
    a[ELParserException] should be thrownBy "${foo${bar}}".el[String]
  }

  it should "be handled correctly when there is a nested attribute definition with string after nested attribute" in {
    a[ELParserException] should be thrownBy "${${bar}foo}".el[String]
  }

  it should "be handled correctly when there is a nested attribute definition with string before and after nested attribute" in {
    a[ELParserException] should be thrownBy "${foo${bar}foo}".el[String]
  }

  it should "be handled correctly when there is a nested attribute definition" in {
    a[ELParserException] should be thrownBy "${${bar}}".el[String]
  }

  it should "be handled correctly when there are several nested attributes" in {
    a[ELParserException] should be thrownBy "${${bar}${bar}}".el[String]
  }
}
