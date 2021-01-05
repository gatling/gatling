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

package io.gatling.core.check.xpath

import java.nio.charset.StandardCharsets.UTF_8

import scala.util.Using

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.commons.util.Io._

import net.sf.saxon.s9api.XdmNode

class XPathExtractorSpec extends BaseSpec with ValidationValues {

  private val namespaces = Map("foo" -> "http://foo/foo")

  private val xmlParsers = new XmlParsers(Long.MaxValue)

  private def dom(file: String): Option[XdmNode] =
    Using.resource(getClass.getResourceAsStream(file)) { is =>
      Some(XmlParsers.parse(is, UTF_8))
    }

  private def testCount(expression: String, file: String, expected: Int): Unit = {
    val extractor = XPathExtractors.count(expression, namespaces, xmlParsers)
    extractor(dom(file)).succeeded shouldBe Some(expected)
  }

  private def testSingle(expression: String, namespaces: Map[String, String], occurrence: Int, file: String, expected: Option[String]): Unit = {
    val extractor = XPathExtractors.find(expression, namespaces, occurrence, xmlParsers)
    extractor(dom(file)).succeeded shouldBe expected
  }

  private def testMultiple(expression: String, namespaces: Map[String, String], file: String, expected: Option[List[String]]): Unit = {
    val extractor = XPathExtractors.findAll(expression, namespaces, xmlParsers)
    extractor(dom(file)).succeeded shouldBe expected
  }

  "count" should "return expected result with anywhere expression" in {
    testCount("//author", "/test.xml", 4)
  }

  it should "return expected result with array expression" in {
    testCount("/test/store/book[3]/author", "/test.xml", 1)
  }

  it should "return Some(0) when no results" in {
    testCount("/foo", "/test.xml", 0)
  }

  "extractSingle" should "return expected result with anywhere expression and rank 0" in {
    testSingle("//author", namespaces, 0, "/test.xml", Some("Nigel Rees"))
  }

  it should "support name()" in {
    testSingle("//*[name()='author']", namespaces, 0, "/test.xml", Some("Nigel Rees"))
  }

  it should "return expected result with anywhere expression and rank 1" in {
    testSingle("//author", namespaces, 1, "/test.xml", Some("Evelyn Waugh"))
  }

  it should "return expected result with array expression" in {
    testSingle("/test/store/book[3]/author", namespaces, 0, "/test.xml", Some("Herman Melville"))
  }

  it should "return expected None with array expression" in {
    testSingle("/test/store/book[3]/author", namespaces, 1, "/test.xml", None)
  }

  it should "return expected result with attribute expression" in {
    testSingle("/test/store/book[@att = 'foo']/title", namespaces, 0, "/test.xml", Some("Sayings of the Century"))
  }

  it should "return expected result with last function expression" in {
    testSingle("//book[last()]/title", namespaces, 0, "/test.xml", Some("The Lord of the Rings"))
  }

  it should "support default namespace" in {
    testSingle("//pre:name", Map("pre" -> "http://schemas.test.com/entityserver/runtime/1.0"), 0, "/test2.xml", Some("HR"))
  }

  "extractMultiple" should "return expected result with anywhere expression" in {
    testMultiple("//author", namespaces, "/test.xml", Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
  }

  it should "return expected result with array expression" in {
    testMultiple("/test/store/book[3]/author", namespaces, "/test.xml", Some(List("Herman Melville")))
  }

  it should "return expected result with anywhere namespaced element" in {
    testMultiple("//foo:bar", namespaces, "/test.xml", Some(List("fooBar")))
  }
}
