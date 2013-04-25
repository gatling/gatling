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
package com.excilys.ebi.gatling.core.util

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.core.util.PaddableStringBuilder.toPaddable

@RunWith(classOf[JUnitRunner])
class PaddableStringBuilderSpec extends Specification {

	"appendLeftPaddedString" should {
		"pad correctly a two digits number" in {
			new StringBuilder().appendLeftPaddedString("12", 6).toString must beEqualTo("    12")
		}

		"not pad when the number of digits is higher than the expected string size" in {
			new StringBuilder().appendLeftPaddedString("123456", 4).toString must beEqualTo("123456")
		}
	}

	"appendLeftPaddedString" should {
		"pad correctly a two digits number" in {
			new StringBuilder().appendRightPaddedString("12", 6).toString must beEqualTo("12    ")
		}

		"not pad when the number of digits is higher than the expected string size" in {
			new StringBuilder().appendRightPaddedString("123456", 4).toString must beEqualTo("123456")
		}
	}
}