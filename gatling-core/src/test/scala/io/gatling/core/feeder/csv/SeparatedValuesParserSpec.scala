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
package io.gatling.core.feeder.csv

import scala.tools.nsc.io.File

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.core.Predef.tsv
import io.gatling.core.config.GatlingConfiguration

@RunWith(classOf[JUnitRunner])
class SeparatedValuesParserSpec extends Specification {

	GatlingConfiguration.setUp()

	"tsv" should {

		"handle file without escape char" in {
			val data = tsv("sample1.tsv").build.toArray

			data must beEqualTo(Array(Map("foo" -> "hello", "bar" -> "world")))
		}

		"handle file with escape char" in {
			val data = tsv("sample2.tsv").build.toArray

			data must beEqualTo(Array(Map("foo" -> "hello", "bar" -> "world")))
		}
	}
}