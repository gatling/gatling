/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import io.gatling.core.test.ValidationSpecification
import io.gatling.core.util.IOHelper.withCloseable

/**
 * @see <a href="http://www.w3.org/TR/selectors/#selectors"/> for more details about the CSS selectors syntax
 */
@RunWith(classOf[JUnitRunner])
class CssExtractorSpec extends ValidationSpecification {

	def prepared(file: String) = withCloseable(getClass.getResourceAsStream(file)) { is =>
		val string = IOUtils.toString(is, StandardCharsets.UTF_8)
		CssExtractor.parse(string)
	}

	"CssExtractor" should {
		"support browser conditional tests and behave as a non-IE browser" in {
			new CountCssExtractor("#helloworld", None).extract(prepared("/IeConditionalTests.html")) must succeedWith(Some(1))
		}
	}

	"CssExtractor count" should {

		"return expected result with a class selector" in {
			new CountCssExtractor(".nav-menu", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some(3))
		}

		"return expected result with an id selector" in {
			new CountCssExtractor("#twitter_button", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some(1))
		}

		"return expected result with an :empty selector" in {
			new CountCssExtractor(".frise:empty", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some(1))
		}

		"return None when the selector doesn't match anything" in {
			new CountCssExtractor("bad_selector", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(None)
		}
	}

	"CssExtractor extractMultiple" should {

		"return expected result with a class selector" in {
			new MultipleCssExtractor("#social", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some(List("Social")))
		}

		"return expected result with an id selector" in {
			new MultipleCssExtractor(".nav", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some(List("Sponsors", "Social")))
		}

		"return expected result with an attribute containg a given substring" in {
			new MultipleCssExtractor(".article a[href*=api]", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some(List("API Documentation")))
		}

		"return expected result with an element being the n-th child of its parent" in {
			new MultipleCssExtractor(".article a:nth-child(2)", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some(List("JMeter's")))
		}

		"return expected result with a predecessor selector" in {
			new MultipleCssExtractor("img ~ p", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some(List("Efficient Load Testing")))
		}

		"return None when the selector doesn't match anything" in {
			new MultipleCssExtractor("bad_selector", None).extract(prepared("/GatlingHomePage.html")) must succeedWith(None)
		}

		"be able to extract a precise node attribute" in {
			new MultipleCssExtractor("#sample_requests", Some("href")).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some(List("http://gatling-tool.org/sample/requests.html")))
		}
	}

	"CssExtractor extractSingle" should {

		"return expected result with a class selector" in {
			new SingleCssExtractor(".nav", None, 1).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some("Social"))
		}

		"return None when the index is out of the range of returned elements" in {
			new SingleCssExtractor(".nav", None, 3).extract(prepared("/GatlingHomePage.html")) must succeedWith(None)
		}

		"return None when the selector doesn't match anything" in {
			new SingleCssExtractor("bad_selector", None, 1).extract(prepared("/GatlingHomePage.html")) must succeedWith(None)
		}

		"be able to extract a precise node attribute" in {
			new SingleCssExtractor(".nav", Some("id"), 1).extract(prepared("/GatlingHomePage.html")) must succeedWith(Some("social"))
		}
	}
}
