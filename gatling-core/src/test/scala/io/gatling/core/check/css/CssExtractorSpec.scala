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

package io.gatling.core.check.css

import java.nio.charset.StandardCharsets.UTF_8

import scala.util.Using

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.commons.util.Io._

import jodd.lagarto.dom.NodeSelector

class CssExtractorSpec extends BaseSpec with ValidationValues {

  private val cssSelectors = new CssSelectors(Long.MaxValue)

  private def prepared(file: String): NodeSelector = Using.resource(getClass.getResourceAsStream(file)) { is =>
    val string = is.toString(UTF_8)
    cssSelectors.parse(string.toCharArray)
  }

  "count" should "support browser conditional tests and behave as a non-IE browser" in {
    val cssExtractor = CssExtractors.count("#helloworld", None, cssSelectors)
    cssExtractor(prepared("/IeConditionalTests.html")).succeeded shouldBe Some(1)
  }

  it should "return expected result with a class selector" in {
    val cssExtractor = CssExtractors.count(".nav-menu", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(3)
  }

  it should "return expected result with an id selector" in {
    val cssExtractor = CssExtractors.count("#twitter_button", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(1)
  }

  it should "return expected result with an :empty selector" in {
    val cssExtractor = CssExtractors.count(".frise:empty", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(1)
  }

  it should "return None when the selector doesn't match anything" in {
    val cssExtractor = CssExtractors.count("bad_selector", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(0)
  }

  "findAll" should "return expected result with a class selector" in {
    val cssExtractor = CssExtractors.findAll[String]("#social", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Social"))
  }

  it should "return expected result with an id selector" in {
    val cssExtractor = CssExtractors.findAll[String](".nav", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Sponsors", "Social"))
  }

  it should "return expected result with an attribute containing a given substring" in {
    val cssExtractor = CssExtractors.findAll[String](".article a[href*=api]", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("API Documentation"))
  }

  it should "return expected result with an element being the n-th child of its parent" in {
    val cssExtractor = CssExtractors.findAll[String](".article a:nth-child(2)", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("JMeter's"))
  }

  it should "return expected result with a predecessor selector" in {
    val cssExtractor = CssExtractors.findAll[String]("img ~ p", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Efficient Load Testing"))
  }

  it should "return None when the selector doesn't match anything" in {
    val cssExtractor = CssExtractors.findAll[String]("bad_selector", None, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "be able to extract a precise node attribute" in {
    val cssExtractor = CssExtractors.findAll[String]("#sample_requests", Some("href"), cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("http://gatling.io/sample/requests.html"))
  }

  "find" should "return expected result with a class selector" in {
    val cssExtractor = CssExtractors.find[String](".nav", None, 1, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some("Social")
  }

  it should "return None when the index is out of the range of returned elements" in {
    val cssExtractor = CssExtractors.find[String](".nav", None, 3, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "return None when the selector doesn't match anything" in {
    val cssExtractor = CssExtractors.find[String]("bad_selector", None, 1, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "be able to extract a precise node attribute" in {
    val cssExtractor = CssExtractors.find[String](".nav", Some("id"), 1, cssSelectors)
    cssExtractor(prepared("/GatlingHomePage.html")).succeeded shouldBe Some("social")
  }

  it should "support filtered value with dots" in {
    val cssExtractor = CssExtractors.find[String]("input[name='javax.faces.ViewState']", Some("value"), 0, cssSelectors)
    cssExtractor(
      cssSelectors.parse(
        """<input type="hidden" name="javax.faces.ViewState" value="foo">""".toCharArray
      )
    ).succeeded shouldBe Some("foo")
  }
}
