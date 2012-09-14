/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.metrics.types

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BucketsSpec extends Specification {

	"getQuantile" should {
		"work when there is no measure" in {
			val buckets = new Buckets(100)
			buckets.getQuantile(91) must beEqualTo(0L)
		}

		"work when there is one measure" in {
			val buckets = new Buckets(100)
			buckets.update(53)
			buckets.getQuantile(91) must beEqualTo(53L)
		}

		"work with a small number of measures" in {
			val buckets = new Buckets(100)
			for (i <- 1 to 100)
				buckets.update(i)

			buckets.getQuantile(91) must beEqualTo(100L)
		}

		"work with a large number of measures" in {
			val buckets = new Buckets(100)
			for (i <- 1 to 10000)
				buckets.update(i)

			buckets.getQuantile(91) must beEqualTo(9200L)
		}

		"work with a low percentiles" in {
			val buckets = new Buckets(100)
			for (i <- 1 to 10000)
				buckets.update(i)

			buckets.getQuantile(0) must beEqualTo(100L)
			buckets.getQuantile(1) must beEqualTo(200L)
		}

		"work with a large percentiles" in {
			val buckets = new Buckets(100)
			for (i <- 1 to 10000)
				buckets.update(i)
			buckets.getQuantile(99) must beEqualTo(10000L)
			buckets.getQuantile(100) must beEqualTo(10000L)
		}
	}
}
