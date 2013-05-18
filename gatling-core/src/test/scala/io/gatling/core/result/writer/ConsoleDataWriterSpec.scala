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
package io.gatling.core.result.writer

import scala.collection.mutable.Map

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.core.config.GatlingConfiguration

@RunWith(classOf[JUnitRunner])
class ConsoleDataWriterSpec extends Specification {

	GatlingConfiguration.setUp()

	val time = new DateTime().withDate(2012, 8, 24).withTime(13, 37, 0, 0)

	def progressBar(summary: ConsoleSummary) = summary.toString.split("\n")(4)

	"console summary progress bar" should {

		"handle it correctly when all the users are waiting" in {

			val counters = new UserCounters(11)

			val summary = ConsoleSummary(10000, Map("request1" -> counters), new RequestCounters, Map.empty, time)
			summary.complete must beFalse
			progressBar(summary) must beEqualTo("[                                                                          ]  0%")
		}

		"handle it correctly when all the users are running" in {

			val counters = new UserCounters(11)
			for (i <- 1 to 11) counters.userStart

			val summary = ConsoleSummary(10000, Map("request1" -> counters), new RequestCounters, Map.empty, time)
			summary.complete must beFalse
			progressBar(summary) must beEqualTo("[--------------------------------------------------------------------------]  0%")
		}

		"handle it correctly when all the users are done" in {

			val counters = new UserCounters(11)
			for (i <- 1 to 11) counters.userStart
			for (i <- 1 to 11) counters.userDone

			val summary = ConsoleSummary(10000, Map("request1" -> counters), new RequestCounters, Map.empty, time)
			summary.complete must beTrue
			progressBar(summary) must beEqualTo("[##########################################################################]100%")
		}

		"handle it correctly when there are running and done users" in {

			val counters = new UserCounters(11)
			for (i <- 1 to 11) counters.userStart
			for (i <- 1 to 10) counters.userDone

			val summary = ConsoleSummary(10000, Map("request1" -> counters), new RequestCounters, Map.empty, time)
			summary.complete must beFalse
			progressBar(summary) must beEqualTo("[###################################################################-------] 90%")
		}
	}
}
