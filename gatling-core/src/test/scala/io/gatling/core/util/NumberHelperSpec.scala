/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.util

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NumberHelperSpec extends Specification {

  import NumberHelper._

  "formatNumberWithSuffix" should {

    "return '1st' for 1" in {
      1.toRank must beEqualTo("1st")
    }

    "return '2nd' for 2" in {
      2.toRank must beEqualTo("2nd")
    }

    "return '3rd' for 3" in {
      3.toRank must beEqualTo("3rd")
    }

    "return '4th' for 4" in {
      4.toRank must beEqualTo("4th")
    }

    "return '11th' for 11" in {
      11.toRank must beEqualTo("11th")
    }

    "return '12th' for 12" in {
      12.toRank must beEqualTo("12th")
    }

    "return '13th' for 13" in {
      13.toRank must beEqualTo("13th")
    }

    "return '21st' for 21" in {
      21.toRank must beEqualTo("21st")
    }

    "return '12341st' for 12341" in {
      12341.toRank must beEqualTo("12341st")
    }

    "return '12311th' for 12311" in {
      12311.toRank must beEqualTo("12311th")
    }
  }
}