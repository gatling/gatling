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

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import io.gatling.core.session.{ el, Session }
import io.gatling.core.session.el._
import io.gatling.core.test.ValidationSpecification
import scala.collection.convert.Wrappers.JListWrapper
import java.util

@RunWith(classOf[JUnitRunner])
class ELSpec extends ValidationSpecification {
  sequential

  "One monovalued Expression" should {

    "return expected result when the variable is the whole string" in {
      val session = Session("scenario", "1", Map("bar" -> "BAR"))
      val expression = "${bar}".el[String]
      expression(session) must succeedWith("BAR")
    }

    "return expected result when the variable is at the end of the string" in {
      val session = Session("scenario", "1", Map("bar" -> "BAR"))
      val expression = "foo${bar}".el[String]
      expression(session) must succeedWith("fooBAR")
    }

    "return expected result when the variable is at the beginning of the string" in {
      val session = Session("scenario", "1", Map("bar" -> "BAR"))
      val expression = "${bar}baz".el[String]
      expression(session) must succeedWith("BARbaz")
    }

    "return expected result when the variable is in the middle of the string" in {
      val session = Session("scenario", "1", Map("bar" -> "BAR"))
      val expression = "foo${bar}baz".el[String]
      expression(session) must succeedWith("fooBARbaz")
    }

    "handle gracefully when an attribute is missing" in {
      val session = Session("scenario", "1", Map("foo" -> "FOO"))
      val expression = "foo${bar}".el[String]
      expression(session) must failWith(ELMessages.undefinedSessionAttributeMessage("bar"))
    }
  }

  "Multivalued Expression" should {

    "return expected result with 2 monovalued expressions" in {
      val session = Session("scenario", "1", Map("foo" -> "FOO", "bar" -> "BAR"))
      val expression = "${foo} ${bar}".el[String]
      expression(session) must succeedWith("FOO BAR")
    }

    "return expected result when used with a static index" in {
      val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2")))
      val expression = "foo${bar(1)}".el[String]
      expression(session) must succeedWith("fooBAR2")
    }
  }

  "'index' function in Expression" should {
    "return n-th element of a list in monovalued expression" in {
      val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2")))
      val expression = "${bar(0)}".el[String]
      expression(session) must succeedWith("BAR1")
    }

    "return expected result when used with resolved index" in {
      val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2"), "baz" -> 1))
      val expression = "{foo${bar(baz)}}".el[String]
      expression(session) must succeedWith("{fooBAR2}")
    }

    "return n-th element of an Array" in {
      val session = Session("scenario", "1", Map("arr" -> Array(1, 2)))
      val expression = "${arr(0)}".el[Int]
      expression(session) must succeedWith(1)
    }

    "return n-th element of JList" in {
      val lst = new util.LinkedList[Int]
      lst.add(1)
      lst.add(2)
      val session = Session("scenario", "1", Map("lst" -> lst))
      val expression = "${lst(0)}".el[Int]
      expression(session) must succeedWith(1)
    }

    "handle gracefully when index in an Array is out of range" in {
      val session = Session("scenario", "1", Map("arr" -> Array(1, 2)))
      val expression = "${arr(2)}".el[Int]
      expression(session) must failWith(ELMessages.undefinedSeqIndexMessage("arr", 2))
    }

    "handle gracefully when index in an JList is out of range" in {
      val lst = new util.LinkedList[Int]
      lst.add(1)
      lst.add(2)
      val session = Session("scenario", "1", Map("lst" -> lst))
      val expression = "${lst(2)}".el[Int]
      expression(session) must failWith(ELMessages.undefinedSeqIndexMessage("lst", 2))
    }

    "handle gracefully when used with static index and missing attribute" in {
      val session = Session("scenario", "1", Map.empty)
      val expression = "foo${bar(1)}".el[String]
      expression(session) must failWith(ELMessages.undefinedSessionAttributeMessage("bar"))
    }

    "handle gracefully when used with static index and empty attribute" in {
      val session = Session("scenario", "1", Map("bar" -> Nil))
      val expression = "foo${bar(1)}".el[String]
      expression(session) must failWith(ELMessages.undefinedSeqIndexMessage("bar", 1))
    }

    "handle gracefully when used with static index and missing index" in {
      val session = Session("scenario", "1", Map("bar" -> List("BAR1")))
      val expression = "foo${bar(1)}".el[String]
      expression(session) must failWith(ELMessages.undefinedSeqIndexMessage("bar", 1))
    }

    "handle gracefully when used with missing resolved index attribute" in {
      val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2")))
      val expression = "{foo${bar(baz)}}".el[String]
      expression(session) must failWith(ELMessages.undefinedSessionAttributeMessage("baz"))
    }

    "handle gracefully value of unsupported type" in {
      val session = Session("scenario", "1", Map("i" -> 1))
      val expression = "${i(0)}".el[Int]
      expression(session) must failWith(ELMessages.indexAccessNotSupportedMessage(1, "i"))
    }
  }

  "'size' function in Expression" should {

    "return correct size for non empty seq" in {
      val session = Session("scenario", "1", Map("bar" -> List("BAR1", "BAR2")))
      val expression = "${bar.size}".el[Int]
      expression(session) must succeedWith(2)
    }

    "return correct size for empty seq" in {
      val session = Session("scenario", "1", Map("bar" -> List()))
      val expression = "${bar.size}".el[Int]
      expression(session) must succeedWith(0)
    }

    "return 0 size for missing attribute" in {
      val session = Session("scenario", "1")
      val expression = "${bar.size}".el[Int]
      expression(session) must failWith(ELMessages.undefinedSessionAttributeMessage("bar"))
    }

    "return correct size for a non empty Array" in {
      val session = Session("scenario", "1", Map("arr" -> Array(1, 2)))
      val expression = "${arr.size}".el[Int]
      expression(session) must succeedWith(2)
    }

    "return correct size for a non empty JList" in {
      val lst = new java.util.LinkedList[Int]
      lst.add(1)
      lst.add(2)
      val session = Session("scenario", "1", Map("lst" -> lst))
      val expression = "${lst.size}".el[Int]
      expression(session) must succeedWith(2)
    }

    "return correct size for a non empty map" in {
      val session = Session("scenario", "1", Map("map" -> Map("key1" -> "val1", "key2" -> "val2")))
      val expression = "${map.size}".el[Int]
      expression(session) must succeedWith(2)
    }

    "return correct size for a non empty JSet" in {
      val set = new java.util.HashSet[Int]
      set.add(1)
      set.add(2)
      val session = Session("scenario", "1", Map("set" -> set))
      val expression = "${set.size}".el[Int]
      expression(session) must succeedWith(2)
    }

    "return correct size for a non empty JMap" in {
      val map = new java.util.HashMap[Int, Int]
      map.put(1, 1)
      map.put(2, 2)
      val session = Session("scenario", "1", Map("map" -> map))
      val expression = "${map.size}".el[Int]
      expression(session) must succeedWith(2)
    }

    "handle gracefully unsupported type" in {
      val session = Session("scenario", "1", Map("i" -> 10))
      val expression = "${i.size}".el[Int]
      expression(session) must failWith(ELMessages.sizeNotSupportedMessage(10, "i"))
    }
  }

  "'random' function in Expression" should {
    "return one of elements of List" in {
      val elements = List("BAR1", "BAR2")
      val session = Session("scenario", "1", Map("bar" -> elements))
      val expression = "${bar.random}".el[String]
      expression(session) must succeedWith("BAR1") or succeedWith("BAR2")
    }

    "return one of elements of JList" in {
      val list = new util.ArrayList[Int]
      list.add(1)
      list.add(2)
      val session = Session("scenario", "1", Map("lst" -> list))
      val expression = "${lst.random}".el[Int]
      expression(session) must succeedWith(1) or succeedWith(2)
    }

    "return one of elements of Array" in {
      val session = Session("scenario", "1", Map("arr" -> Array(1, 2)))
      val expression = "${arr.random}".el[Int]
      expression(session) must succeedWith(1) or succeedWith(2)
    }

    "handle unsupported type" in {
      val session = Session("scenario", "1", Map("i" -> 10))
      val expression = "${i.random}".el[Int]
      expression(session) must failWith(el.ELMessages.randomNotSupportedMessage(10, "i"))
    }
  }

  "access map in Expression" should {
    "return value by key" in {
      val map = Map("key1" -> "val1", "key2" -> "val2")
      val session = Session("scenario", "1", Map("map" -> map))
      val expression = "${map.key1}".el[String]
      expression(session) must succeedWith("val1")
    }

    "return value by key from JMap" in {
      val map = new java.util.HashMap[String, Int]
      map.put("key1", 1)
      map.put("key2", 2)
      val session = Session("scenario", "1", Map("map" -> map))
      val expression = "${map.key1}".el[Int]
      expression(session) must succeedWith(1)
    }

    "handle missing map correctly" in {
      val session = Session("scenario", "1")
      val expression = "${map.key1}".el[String]
      expression(session) must failWith(ELMessages.undefinedSessionAttributeMessage("map"))
    }

    "handle nested map access" in {
      val map = Map("key" -> Map("subKey" -> "val"))
      val session = Session("scenario", "1", Map("map" -> map))
      val expression = "${map.key.subKey}".el[String]
      expression(session) must succeedWith("val")
    }

    "handle missing value correctly" in {
      val map = Map("key" -> "val")
      val session = Session("scenario", "1", Map("map" -> map))
      val expression = "${map.nonexisting}".el[String]
      expression(session) must failWith(ELMessages.undefinedMapKeyMessage("map", "nonexisting"))
    }

    "handle missing value in JMap correctly" in {
      val map = new java.util.HashMap[String, Int]
      map.put("key1", 1)
      val session = Session("scenario", "1", Map("map" -> map))
      val expression = "${map.nonexisting}".el[Int]
      expression(session) must failWith(ELMessages.undefinedMapKeyMessage("map", "nonexisting"))
    }

    "handle wrong type correctly" in {
      val session = Session("scenario", "1", Map("i" -> 1))
      val expression = "${i.key}".el[Int]
      expression(session) must failWith(ELMessages.accessByKeyNotSupportedMessage(1, "i"))
    }
  }

  "multiple level access" should {
    "return an element of a sub-list" in {
      val lst = List(List(1, 2), List(3, 4))
      val session = Session("scenario", "1", Map("lst" -> lst))
      val expression = "${lst(0)(0)}".el[Int]
      expression(session) must succeedWith(1)
    }

    "return key of a map in a list" in {
      val lst = List(Map("key" -> "val"))
      val session = Session("scenario", "1", Map("lst" -> lst))
      val expression = "${lst(0).key}".el[String]
      expression(session) must succeedWith("val")
    }

    "return an element of a list from a map" in {
      val map = Map("lst" -> List(1, 2))
      val session = Session("scenario", "1", Map("map" -> map))
      val expression = "${map.lst(0)}".el[Int]
      expression(session) must succeedWith(1)
    }

    "return a value from a sub-map" in {
      val map = Map("subMap" -> Map("key" -> "val"))
      val session = Session("scenario", "1", Map("map" -> map))
      val expression = "${map.subMap.key}".el[String]
      expression(session) must succeedWith("val")
    }

    "return a value from a sub-map" in {
      val map = Map("subMap" -> Map("key" -> "val"))
      val session = Session("scenario", "1", Map("map" -> map))
      val expression = "${map.subMap.key}".el[String]
      expression(session) must succeedWith("val")
    }

    "return a value from a random list" in {
      val lst = List(List(1, 2), List(3, 4))
      val session = Session("scenario", "1", Map("lst" -> lst))
      val expression = "${lst.random(0)}".el[Int]
      expression(session) must succeedWith(1) or succeedWith(3)
    }

    "return size of a sub-list" in {
      val lst = List(List(1, 2), List(3, 4, 5))
      val session = Session("scenario", "1", Map("lst" -> lst))
      val expression = "${lst(1).size}".el[Int]
      expression(session) must succeedWith(3)
    }

    "return size of a sub-list" in {
      val lst = List(List(1, 2), List(3, 4, 5))
      val session = Session("scenario", "1", Map("lst" -> lst))
      val expression = "${lst(1).size}".el[Int]
      expression(session) must succeedWith(3)
    }
  }

  "Malformed Expression" should {

    "be handled correctly when an attribute name is missing" in {
      "foo${}bar".el[String] must throwA[ELMissingAttributeName]
    }

    "be handled correctly when there is a nested attribute definition" in {
      "${foo${bar}}".el[String] must throwA[ELParserException]
    }
  }
}