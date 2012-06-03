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
package com.excilys.ebi.gatling.charts.view.util

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.charts.view.util.ViewHelper.ordinalNumberSuffix

@RunWith(classOf[JUnitRunner])
class ViewHelperSpec extends Specification {

	"ordinalNumberSuffix" should {

		"return 'st' for 1" in {
			ordinalNumberSuffix(21) must beEqualTo("st")
		}

		"return 'nd' for 2" in {
			ordinalNumberSuffix(2) must beEqualTo("nd")
		}

		"return 'rd' for 3" in {
			ordinalNumberSuffix(3) must beEqualTo("rd")
		}

		"return 'th' for 4" in {
			ordinalNumberSuffix(4) must beEqualTo("th")
		}

		"return 'th' for 11" in {
			ordinalNumberSuffix(11) must beEqualTo("th")
		}

		"return 'th' for 12" in {
			ordinalNumberSuffix(12) must beEqualTo("th")
		}

		"return 'th' for 13" in {
			ordinalNumberSuffix(13) must beEqualTo("th")
		}

		"return 'st' for 21" in {
			ordinalNumberSuffix(21) must beEqualTo("st")
		}

		"return 'st' for 12341" in {
			ordinalNumberSuffix(12341) must beEqualTo("st")
		}

		"return 'st' for 12311" in {
			ordinalNumberSuffix(12311) must beEqualTo("th")
		}
	}
}