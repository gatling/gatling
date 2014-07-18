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
package io.gatling.core.check.extractor.xpath

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.test.ValidationSpecification
import io.gatling.core.util.IO._

@RunWith(classOf[JUnitRunner])
class XPathExtractorSpec extends ValidationSpecification {

  GatlingConfiguration.setUp()

  val namespaces = List("foo" -> "http://foo/foo")

  def xmdNode(file: String) =
    withCloseable(getClass.getResourceAsStream(file)) { is =>
      Some(SaxonXPathExtractor.parse(is))
    }

  def document(file: String) =
    withCloseable(getClass.getResourceAsStream(file)) { is =>
      Some(JDKXPathExtractor.parse(is))
    }

  "count" should {

    "return expected result with anywhere expression" in {
      val expression = "//author"
      val file = "/test.xml"
      val expected = 4

      new SaxonXPathExtractor.CountXPathExtractor(expression, namespaces)(xmdNode(file)) must succeedWith(Some(expected))
      new JDKXPathExtractor.CountXPathExtractor(expression, namespaces)(document(file)) must succeedWith(Some(expected))
    }

    "return expected result with array expression" in {

      val expression = "/test/store/book[3]/author"
      val file = "/test.xml"
      val expected = 1

      new SaxonXPathExtractor.CountXPathExtractor(expression, namespaces)(xmdNode(file)) must succeedWith(Some(expected))
      new JDKXPathExtractor.CountXPathExtractor(expression, namespaces)(document(file)) must succeedWith(Some(expected))
    }

    "return Some(0) when no results" in {

      val expression = "/foo"
      val file = "/test.xml"
      val expected = 0

      new SaxonXPathExtractor.CountXPathExtractor(expression, namespaces)(xmdNode(file)) must succeedWith(Some(expected))
      new JDKXPathExtractor.CountXPathExtractor(expression, namespaces)(document(file)) must succeedWith(Some(expected))
    }
  }

  "extractSingle" should {

      def single(expression: String, rank: Int, file: String, expected: Option[String]): Unit = {

      }

    "return expected result with anywhere expression and rank 0" in {

      val expression = "//author"
      val rank = 0
      val file = "/test.xml"
      val expected = Some("Nigel Rees")

      new SaxonXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(document(file)) must succeedWith(expected)
    }

    "support name()" in {

      val expression = "//*[name()='author']"
      val rank = 0
      val file = "/test.xml"
      val expected = Some("Nigel Rees")

      new SaxonXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(document(file)) must succeedWith(expected)
    }

    "return expected result with anywhere expression and rank 1" in {

      val expression = "//author"
      val rank = 1
      val file = "/test.xml"
      val expected = Some("Evelyn Waugh")

      new SaxonXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(document(file)) must succeedWith(expected)
    }

    "return expected result with array expression" in {

      val expression = "/test/store/book[3]/author"
      val rank = 0
      val file = "/test.xml"
      val expected = Some("Herman Melville")

      new SaxonXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(document(file)) must succeedWith(expected)
    }

    "return expected None with array expression" in {

      val expression = "/test/store/book[3]/author"
      val rank = 1
      val file = "/test.xml"
      val expected = None

      new SaxonXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(document(file)) must succeedWith(expected)
    }

    "return expected result with attribute expression" in {

      val expression = "/test/store/book[@att = 'foo']/title"
      val rank = 0
      val file = "/test.xml"
      val expected = Some("Sayings of the Century")

      new SaxonXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(document(file)) must succeedWith(expected)
    }

    "return expected result with last function expression" in {

      val expression = "//book[last()]/title"
      val rank = 0
      val file = "/test.xml"
      val expected = Some("The Lord of the Rings")

      new SaxonXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(document(file)) must succeedWith(expected)
    }

    "support default namespace" in {

      val expression = "//pre:name"
      val rank = 0
      val file = "/test2.xml"
      val expected = Some("HR")
      val namespaces = List("pre" -> "http://schemas.test.com/entityserver/runtime/1.0")

      new SaxonXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.SingleXPathExtractor(expression, namespaces, rank)(document(file)) must succeedWith(expected)
    }
  }

  "extractMultiple" should {

    "return expected result with anywhere expression" in {

      val expression = "//author"
      val file = "/test.xml"
      val expected = Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien"))

      new SaxonXPathExtractor.MultipleXPathExtractor(expression, namespaces)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.MultipleXPathExtractor(expression, namespaces)(document(file)) must succeedWith(expected)
    }

    "return expected result with array expression" in {

      val expression = "/test/store/book[3]/author"
      val file = "/test.xml"
      val expected = Some(List("Herman Melville"))

      new SaxonXPathExtractor.MultipleXPathExtractor(expression, namespaces)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.MultipleXPathExtractor(expression, namespaces)(document(file)) must succeedWith(expected)
    }

    "return expected result with anywhere namespaced element" in {

      val expression = "//foo:bar"
      val file = "/test.xml"
      val expected = Some(List("fooBar"))

      new SaxonXPathExtractor.MultipleXPathExtractor(expression, namespaces)(xmdNode(file)) must succeedWith(expected)
      new JDKXPathExtractor.MultipleXPathExtractor(expression, namespaces)(document(file)) must succeedWith(expected)
    }
  }
}
