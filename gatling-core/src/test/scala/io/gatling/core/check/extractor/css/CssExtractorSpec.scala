/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import jodd.lagarto.dom.NodeSelector

import io.gatling.BaseSpec
import io.gatling.core.ValidationValues
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.Io._

class CssExtractorSpec extends BaseSpec with ValidationValues {

  implicit val configuration = GatlingConfiguration.loadForTest()
  implicit val cssSelectors = new CssSelectors
  val extractorFactory = new CssExtractorFactory
  import extractorFactory._

  def prepared(file: String): NodeSelector = withCloseable(getClass.getResourceAsStream(file)) { is =>
    val string = is.toString(UTF_8)
    cssSelectors.parse(string)
  }

  "CssExtractor" should "support browser conditional tests and behave as a non-IE browser" in {
    newCountExtractor("#helloworld", None).extract(prepared("/IeConditionalTests.html")).succeeded shouldBe Some(1)
  }

  it should "return expected result with a class selector" in {
    newCountExtractor(".nav-menu", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(3)
  }

  it should "return expected result with an id selector" in {
    newCountExtractor("#twitter_button", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(1)
  }

  it should "return expected result with an :empty selector" in {
    newCountExtractor(".frise:empty", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(1)
  }

  it should "return None when the selector doesn't match anything" in {
    newCountExtractor("bad_selector", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(0)
  }

  "CssExtractor extractMultiple" should "return expected result with a class selector" in {
    newMultipleExtractor[String]("#social", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Social"))
  }

  it should "return expected result with an id selector" in {
    newMultipleExtractor[String](".nav", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Sponsors", "Social"))
  }

  it should "return expected result with an attribute containg a given substring" in {
    newMultipleExtractor[String](".article a[href*=api]", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("API Documentation"))
  }

  it should "return expected result with an element being the n-th child of its parent" in {
    newMultipleExtractor[String](".article a:nth-child(2)", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("JMeter's"))
  }

  it should "return expected result with a predecessor selector" in {
    newMultipleExtractor[String]("img ~ p", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("Efficient Load Testing"))
  }

  it should "return None when the selector doesn't match anything" in {
    newMultipleExtractor[String]("bad_selector", None).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "be able to extract a precise node attribute" in {
    newMultipleExtractor[String]("#sample_requests", Some("href")).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some(List("http://gatling.io/sample/requests.html"))
  }

  "CssExtractor extractSingle" should "return expected result with a class selector" in {
    newSingleExtractor[String]((".nav", None), 1).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some("Social")
  }

  it should "return None when the index is out of the range of returned elements" in {
    newSingleExtractor[String]((".nav", None), 3).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "return None when the selector doesn't match anything" in {
    newSingleExtractor[String](("bad_selector", None), 1).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe None
  }

  it should "be able to extract a precise node attribute" in {
    newSingleExtractor[String]((".nav", Some("id")), 1).extract(prepared("/GatlingHomePage.html")).succeeded shouldBe Some("social")
  }
}
