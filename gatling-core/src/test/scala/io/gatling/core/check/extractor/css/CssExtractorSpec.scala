/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

package io.gatling.core.check.extractor.css

import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.{ ValidationValues, BaseSpec }
import io.gatling.commons.util.Io._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.check.extractor.css.CssExtractorFactory._

import jodd.lagarto.dom.NodeSelector

class CssExtractorSpec extends BaseSpec with ValidationValues {

  private implicit val configuration = GatlingConfiguration.loadForTest()
  private val cssSelectors = new CssSelectors

  private def prepared(file: String): NodeSelector = withCloseable(getClass.getResourceAsStream(file)) { is =>
    val string = is.toString(UTF_8)
    cssSelectors.parse(string.toCharArray)
  }

  "CssExtractor" should "support browser conditional tests and behave as a non-IE browser" in {
    val cssExtractor = newCssCountExtractor("#helloworld", None, cssSelectors)
    cssExtractor(prepared("/IeConditionalTests.html")).succeeded shouldBe Some(1)
  }

  it should "return expected result with a class selector" in {
    val cssExtractor = newCssCountExtractor(".nav-menu", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(3)
  }

  it should "return expected result with an id selector" in {
    val cssExtractor = newCssCountExtractor("#twitter_button", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(1)
  }

  it should "return expected result with an :empty selector" in {
    val cssExtractor = newCssCountExtractor(".frise:empty", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(1)
  }

  it should "return None when the selector doesn't match anything" in {
    val cssExtractor = newCssCountExtractor("bad_selector", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(0)
  }

  "CssExtractor extractMultiple" should "return expected result with a class selector" in {
    val cssExtractor = newCssMultipleExtractor[String]("#social", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Social"))
  }

  it should "return expected result with an id selector" in {
    val cssExtractor = newCssMultipleExtractor[String](".nav", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Sponsors", "Social"))
  }

  it should "return expected result with an attribute containg a given substring" in {
    val cssExtractor = newCssMultipleExtractor[String](".article a[href*=api]", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("API Documentation"))
  }

  it should "return expected result with an element being the n-th child of its parent" in {
    val cssExtractor = newCssMultipleExtractor[String](".article a:nth-child(2)", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("JMeter's"))
  }

  it should "return expected result with a predecessor selector" in {
    val cssExtractor = newCssMultipleExtractor[String]("img ~ p", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Efficient Load Testing"))
  }

  it should "return None when the selector doesn't match anything" in {
    val cssExtractor = newCssMultipleExtractor[String]("bad_selector", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "be able to extract a precise node attribute" in {
    val cssExtractor = newCssMultipleExtractor[String]("#sample_requests", Some("href"), cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("http://gatling.io/sample/requests.html"))
  }

  "CssExtractor extractSingle" should "return expected result with a class selector" in {
    val cssExtractor = newCssSingleExtractor[String](".nav", None, 1, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some("Social")
  }

  it should "return None when the index is out of the range of returned elements" in {
    val cssExtractor = newCssSingleExtractor[String](".nav", None, 3, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "return None when the selector doesn't match anything" in {
    val cssExtractor = newCssSingleExtractor[String]("bad_selector", None, 1, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "be able to extract a precise node attribute" in {
    val cssExtractor = newCssSingleExtractor[String](".nav", Some("id"), 1, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some("social")
  }

  it should "support filtered value with dots" in {
    val cssExtractor = newCssSingleExtractor[String]("input[name='javax.faces.ViewState']", Some("value"), 0, cssSelectors)
    cssExtractor(cssSelectors.parse(
      """<input type="hidden" name="javax.faces.ViewState" value="foo">""".toCharArray
    )).succeeded shouldBe Some("foo")
  }
}
