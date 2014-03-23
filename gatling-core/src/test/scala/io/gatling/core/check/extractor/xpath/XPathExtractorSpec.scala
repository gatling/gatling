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
import io.gatling.core.util.IOHelper.withCloseable

@RunWith(classOf[JUnitRunner])
class XPathExtractorSpec extends ValidationSpecification {

  GatlingConfiguration.setUp()

  val namespaces = List("foo" -> "http://foo/foo")

  def prepared(file: String) = {
    GatlingConfiguration.setUp()
    withCloseable(getClass.getResourceAsStream(file)) { is =>
      Some(XPathExtractor.parse(is))
    }
  }

  "count" should {

    "return expected result with anywhere expression" in {
      new CountXPathExtractor("//author", namespaces)(prepared("/test.xml")) must succeedWith(Some(4))
    }

    "return expected result with array expression" in {
      new CountXPathExtractor("/test/store/book[3]/author", namespaces)(prepared("/test.xml")) must succeedWith(Some(1))
    }

    "return Some(0) when no results" in {
      new CountXPathExtractor("/foo", namespaces)(prepared("/test.xml")) must succeedWith(Some(0))
    }
  }

  "extractSingle" should {

    "return expected result with anywhere expression and rank 0" in {
      new SingleXPathExtractor("//author", namespaces, 0)(prepared("/test.xml")) must succeedWith(Some("Nigel Rees"))
    }

    "support name()" in {
      new SingleXPathExtractor("//*[name()='author']", namespaces, 0)(prepared("/test.xml")) must succeedWith(Some("Nigel Rees"))
    }

    "return expected result with anywhere expression and rank 1" in {
      new SingleXPathExtractor("//author", namespaces, 0).extract(prepared("/test.xml")) must succeedWith(Some("Nigel Rees"))
    }

    "return expected result with array expression" in {
      new SingleXPathExtractor("/test/store/book[3]/author", namespaces, 0)(prepared("/test.xml")) must succeedWith(Some("Herman Melville"))
    }

    "return expected None with array expression" in {
      new SingleXPathExtractor("/test/store/book[3]/author", namespaces, 1)(prepared("/test.xml")) must succeedWith(None)
    }

    "return expected result with attribute expression" in {
      new SingleXPathExtractor("/test/store/book[@att = 'foo']/title", namespaces, 0)(prepared("/test.xml")) must succeedWith(Some("Sayings of the Century"))
    }

    "return expected result with last function expression" in {
      new SingleXPathExtractor("//book[last()]/title", namespaces, 0)(prepared("/test.xml")) must succeedWith(Some("The Lord of the Rings"))
    }

    "support default namespace" in {
      new SingleXPathExtractor("//pre:name", List("pre" -> "http://schemas.test.com/entityserver/runtime/1.0"), 0)(prepared("/test2.xml")) must succeedWith(Some("HR"))
    }
  }

  "extractMultiple" should {

    "return expected result with anywhere expression" in {
      new MultipleXPathExtractor("//author", namespaces)(prepared("/test.xml")) must succeedWith(Some(List("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")))
    }

    "return expected result with array expression" in {
      new MultipleXPathExtractor("/test/store/book[3]/author", namespaces)(prepared("/test.xml")) must succeedWith(Some(List("Herman Melville")))
    }

    "return expected result with anywhere namespaced element" in {
      new MultipleXPathExtractor("//foo:bar", namespaces)(prepared("/test.xml")) must succeedWith(Some(List("fooBar")))
    }
  }
}
