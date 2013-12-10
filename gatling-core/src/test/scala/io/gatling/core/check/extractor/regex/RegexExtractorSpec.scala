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
package io.gatling.core.check.extractor.regex

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.test.ValidationSpecification

@RunWith(classOf[JUnitRunner])
class RegexExtractorSpec extends ValidationSpecification {

	GatlingConfiguration.setUp()

	"extractMultiple" should {

		"return expected result with anywhere expression" in {

			val stringRegexExtractor = new MultipleRegexExtractor[String](""""SearchDefinitionID\\":(\d*)""")

			stringRegexExtractor("""{"id":"1072920417","result":"[{\"SearchDefinitionID\":116},{\"SearchDefinitionID\":108}]","error":null}""") must succeedWith(Some(List("116", "108")))
		}
	}
}
