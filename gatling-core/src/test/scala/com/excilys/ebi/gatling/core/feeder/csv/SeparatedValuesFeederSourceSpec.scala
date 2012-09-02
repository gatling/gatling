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
package com.excilys.ebi.gatling.core.feeder.csv

import java.io.File
import java.util.{ HashMap => JHashMap }

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.core.config.GatlingConfiguration
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR

@RunWith(classOf[JUnitRunner])
class SeparatedValuesFeederSourceSpec extends Specification {

	"tsv" should {

		GatlingConfiguration.setUp(new JHashMap)

		"handle file without escape char" in {
			val file = new File("src/test/resources/sample1.tsv")

			val values = new SeparatedValuesFeederSource(file, TABULATION_SEPARATOR, None).values

			values.size must beEqualTo(1)
			values(0).get("foo") must beEqualTo(Some("hello"))
			values(0).get("bar") must beEqualTo(Some("world"))
		}

		"handle file with escape char" in {
			val file = new File("src/test/resources/sample2.tsv")

			val values = new SeparatedValuesFeederSource(file, TABULATION_SEPARATOR, Some("'")).values

			values.size must beEqualTo(1)
			values(0).get("foo") must beEqualTo(Some("hello"))
			values(0).get("bar") must beEqualTo(Some("world"))
		}
	}
}