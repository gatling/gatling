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
package io.gatling.core.check.extractor.xpath

import io.gatling.{ ValidationValues, BaseSpec }
import io.gatling.commons.util.Io._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.check.extractor.xpath.XPathExtractorFactory._

import org.xml.sax.InputSource

abstract class XPathExtractorSpec extends BaseSpec with ValidationValues {

  implicit val configuration = GatlingConfiguration.loadForTest()
  val namespaces = List("foo" -> "http://foo/foo")

  val xmlParsers = new XmlParsers

  def dom(file: String): Option[Dom]

  def testCount(expression: String, file: String, expected: Int): Unit = {
    val extractor = newXpathCountExtractor(expression, namespaces, xmlParsers)
    extractor(dom(file)).succeeded shouldBe Some(expected)
  }

  def testSingle(expression: String, namespaces: List[(String, String)], occurrence: Int, file: String, expected: Option[String]): Unit = {
    val extractor = newXpathSingleExtractor(expression, namespaces, occurrence, xmlParsers)
    extractor(dom(file)).succeeded shouldBe expected
  }

  def testMultiple(expression: String, namespaces: List[(String, String)], file: String, expected: Option[List[String]]): Unit = {
    val extractor = newXpathMultipleExtractor(expression, namespaces, xmlParsers)
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
    testSingle("//pre:name", List("pre" -> "http://schemas.test.com/entityserver/runtime/1.0"), 0, "/test2.xml", Some("HR"))
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

class SaxonXPathExtractorSpec extends XPathExtractorSpec {

  def dom(file: String): Option[Dom] =
    withCloseable(getClass.getResourceAsStream(file)) { is =>
      Some(SaxonDom(xmlParsers.saxon.parse(new InputSource(is))))
    }
}

class JdkXPathExtractorSpec extends XPathExtractorSpec {

  def dom(file: String): Option[Dom] =
    withCloseable(getClass.getResourceAsStream(file)) { is =>
      Some(JdkDom(xmlParsers.jdk.parse(new InputSource(is))))
    }
}
