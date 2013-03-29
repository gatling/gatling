/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.nio.CharBuffer

import scala.io.{ Codec, Source }

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import io.gatling.core.test.ValidationSpecification
import io.gatling.core.util.IOHelper.withSource

/**
 * @see <a href="http://www.w3.org/TR/selectors/#selectors"/> for more details about the CSS selectors syntax
 */
@RunWith(classOf[JUnitRunner])
class CssExtractorsSpec extends ValidationSpecification {

	def prepared(file: String) = {
		withSource(Source.fromInputStream(getClass.getResourceAsStream(file))(Codec.UTF8)) { source =>
			CssExtractors.parse(CharBuffer.wrap(source.mkString))
		}
	}

	"CssExtractor" should {
		"support browser conditional tests and behave as a non-IE browser" in {
			CssExtractors.count(None)(prepared("/IeConditionalTests.html"), "#helloworld") must succeedWith(Some(1))
		}
	}

	"count" should {

		"return expected result with a class selector" in {
			CssExtractors.count(None)(prepared("/GatlingHomePage.html"), ".nav-menu") must succeedWith(Some(3))
		}

		"return expected result with an id selector" in {
			CssExtractors.count(None)(prepared("/GatlingHomePage.html"), "#twitter_button") must succeedWith(Some(1))
		}

		"return expected result with an :empty selector" in {
			CssExtractors.count(None)(prepared("/GatlingHomePage.html"), ".frise:empty") must succeedWith(Some(1))
		}

		"return None when the selector doesn't match anything" in {
			CssExtractors.count(None)(prepared("/GatlingHomePage.html"), "bad_selector") must succeedWith(None)
		}
	}

	"extractMultiple" should {

		"return expected result with a class selector" in {
			CssExtractors.extractMultiple(None)(prepared("/GatlingHomePage.html"), "#social") must succeedWith(Some(List("Social")))
		}

		"return expected result with an id selector" in {
			CssExtractors.extractMultiple(None)(prepared("/GatlingHomePage.html"), ".nav") must succeedWith(Some(List("Sponsors", "Social")))
		}

		"return expected result with an attribute containg a given substring" in {
			CssExtractors.extractMultiple(None)(prepared("/GatlingHomePage.html"), ".article a[href*=api]") must succeedWith(Some(List("API Documentation")))
		}

		"return expected result with an element being the n-th child of its parent" in {
			CssExtractors.extractMultiple(None)(prepared("/GatlingHomePage.html"), ".article a:nth-child(2)") must succeedWith(Some(List("JMeter's")))
		}

		"return expected result with a predecessor selector" in {
			CssExtractors.extractMultiple(None)(prepared("/GatlingHomePage.html"), "img ~ p") must succeedWith(Some(List("Efficient Load Testing")))
		}

		"return None when the selector doesn't match anything" in {
			CssExtractors.extractMultiple(None)(prepared("/GatlingHomePage.html"), "bad_selector") must succeedWith(None)
		}

		"be able to extract a precise node attribute" in {
			CssExtractors.extractMultiple(Some("href"))(prepared("/GatlingHomePage.html"), "#sample_requests") must succeedWith(Some(List("http://gatling-tool.org/sample/requests.html")))
		}
	}

	"extractOne" should {

		"return expected result with a class selector" in {
			CssExtractors.extractOne(None)(1)(prepared("/GatlingHomePage.html"), ".nav") must succeedWith(Some("Social"))
		}

		"return None when the index is out of the range of returned elements" in {
			CssExtractors.extractOne(None)(3)(prepared("/GatlingHomePage.html"), ".nav") must succeedWith(None)
		}

		"return None when the selector doesn't match anything" in {
			CssExtractors.extractOne(None)(1)(prepared("/GatlingHomePage.html"), "bad_selector") must succeedWith(None)
		}

		"be able to extract a precise node attribute" in {
			CssExtractors.extractOne(Some("id"))(1)(prepared("/GatlingHomePage.html"), ".nav") must succeedWith(Some("social"))
		}
	}
}


