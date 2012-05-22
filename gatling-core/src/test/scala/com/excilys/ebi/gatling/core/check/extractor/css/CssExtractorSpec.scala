/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.check.extractor.css

import scala.io.Source

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import CssExtractorSpec.extractor

object CssExtractorSpec {

	val html = Source.fromInputStream(getClass.getResourceAsStream("/GatlingHomePage.html")).mkString

	val extractor = new CssExtractor(html)
}

/**
 * @see <a href="http://www.w3.org/TR/selectors/#selectors"/> for more details about the CSS selectors syntax
 */
@RunWith(classOf[JUnitRunner])
class CssExtractorSpec extends Specification {

	"count" should {

		"return expected result with a class selector" in {
			extractor.count(".nav-menu") must beEqualTo(Some(3))
		}

		"return expected result with an id selector" in {
			extractor.count("#twitter_button") must beEqualTo(Some(1))
		}

		"return expected result with an :empty selector" in {
			extractor.count(".frise:empty") must beEqualTo(Some(1))
		}

		"return None when the selector doesn't match anything" in {
			extractor.count("bad_selector") must beEqualTo(None)
		}

	}

	"extractMultiple" should {

		"return expected result with a class selector" in {
			extractor.extractMultiple("#social") must beEqualTo(Some(List("Social")))
		}

		"return expected result with an id selector" in {
			extractor.extractMultiple(".nav") must beEqualTo(Some(List("Sponsors", "Social")))
		}

		"return expected result with an attribute containg a given substring" in {
			extractor.extractMultiple(".article a[href*=api]") must beEqualTo(Some(List("API Documentation")))
		}

		"return expected result with an element being the n-th child of its parent" in {
			extractor.extractMultiple(".article a:nth-child(2)") must beEqualTo(Some(List("JMeter's")))
		}

		"return expected result with a predecessor selector" in {
			extractor.extractMultiple("img ~ p") must beEqualTo(Some(List("Efficient Load Testing")))
		}

		"return None when the selector doesn't match anything" in {
			extractor.extractMultiple("bad_selector") must beEqualTo(None)
		}
	}

	"extractOne" should {

		"return expected result with a class selector" in {
			extractor.extractOne(1)(".nav") must beEqualTo(Some("Social"))
		}

		"return None when the index is out of the range of returned elements" in {
			extractor.extractOne(3)(".nav") must beEqualTo(None)
		}

		"return None when the selector doesn't match anything" in {
			extractor.extractOne(1)("bad_selector") must beEqualTo(None)
		}
	}
}


