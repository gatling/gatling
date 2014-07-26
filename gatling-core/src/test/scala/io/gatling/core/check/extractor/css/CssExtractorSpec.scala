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
package io.gatling.core.check.extractor.css

import java.nio.charset.StandardCharsets

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.junit.JUnitRunner

import jodd.lagarto.dom.NodeSelector
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.test.ValidationValues
import io.gatling.core.util.IO._

/**
 * @see <a href="http://www.w3.org/TR/selectors/#selectors"/> for more details about the CSS selectors syntax
 */
@RunWith(classOf[JUnitRunner])
class CssExtractorSpec extends FlatSpec with Matchers with ValidationValues {

  GatlingConfiguration.setUp()

  def prepared(file: String): NodeSelector = withCloseable(getClass.getResourceAsStream(file)) { is =>
    val string = is.toString(StandardCharsets.UTF_8)
    CssExtractor.parse(string)
  }

  "CssExtractor" should "support browser conditional tests and behave as a non-IE browser" in {
    new CountCssExtractor("#helloworld", None).extract(prepared("/IeConditionalTests.html")).succeeded shouldBe Some(1)
  }

  "CssExtractor count" should "return expected result with a class selector" in {
    new CountCssExtractor(".nav-menu", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(3)
  }

  it should "return expected result with an id selector" in {
    new CountCssExtractor("#twitter_button", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(1)
  }

  it should "return expected result with an :empty selector" in {
    new CountCssExtractor(".frise:empty", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(1)
  }

  it should "return None when the selector doesn't match anything" in {
    new CountCssExtractor("bad_selector", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(0)
  }

  "CssExtractor extractMultiple" should "return expected result with a class selector" in {
    new MultipleCssExtractor("#social", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Social"))
  }

  it should "return expected result with an id selector" in {
    new MultipleCssExtractor(".nav", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Sponsors", "Social"))
  }

  it should "return expected result with an attribute containg a given substring" in {
    new MultipleCssExtractor(".article a[href*=api]", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("API Documentation"))
  }

  it should "return expected result with an element being the n-th child of its parent" in {
    new MultipleCssExtractor(".article a:nth-child(2)", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("JMeter's"))
  }

  it should "return expected result with a predecessor selector" in {
    new MultipleCssExtractor("img ~ p", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Efficient Load Testing"))
  }

  it should "return None when the selector doesn't match anything" in {
    new MultipleCssExtractor("bad_selector", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "be able to extract a precise node attribute" in {
    new MultipleCssExtractor("#sample_requests", Some("href")).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("http://gatling-tool.org/sample/requests.html"))
  }

  "CssExtractor extractSingle" should "return expected result with a class selector" in {
    new SingleCssExtractor(".nav", None, 1).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some("Social")
  }

  it should "return None when the index is out of the range of returned elements" in {
    new SingleCssExtractor(".nav", None, 3).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "return None when the selector doesn't match anything" in {
    new SingleCssExtractor("bad_selector", None, 1).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "be able to extract a precise node attribute" in {
    new SingleCssExtractor(".nav", Some("id"), 1).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some("social")
  }
}
