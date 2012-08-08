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
package com.excilys.ebi.gatling.core.result.writer

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

@RunWith(classOf[JUnitRunner])
class ConsoleDataWriterSpec extends Specification {

	"console summary progress bar" should {

		"handle it correctly when all the users are waiting" in {
			val consoleSummary = new ConsoleSummary(25)
			consoleSummary.appendUsersProgressBar(new UserCounters(11))

			consoleSummary.toString must beEqualTo("Users  : [          ]  0%" + END_OF_LINE)
		}

		"handle it correctly when all the users are running" in {
			val consoleSummary = new ConsoleSummary(25)
			val counters = new UserCounters(11)
			for (i <- 1 to 11) counters.userStart

			consoleSummary.appendUsersProgressBar(counters)

			consoleSummary.toString must beEqualTo("Users  : [----------]  0%" + END_OF_LINE)
		}

		"handle it correctly when all the users are done" in {
			val consoleSummary = new ConsoleSummary(25)
			val counters = new UserCounters(11)
			for (i <- 1 to 11) counters.userStart
			for (i <- 1 to 11) counters.userDone

			consoleSummary.appendUsersProgressBar(counters)

			consoleSummary.toString must beEqualTo("Users  : [##########]100%" + END_OF_LINE)
		}

		"handle it correctly when there are running and done users" in {
			val consoleSummary = new ConsoleSummary(25)

			val counters = new UserCounters(11)
			for (i <- 1 to 11) counters.userStart
			for (i <- 1 to 10) counters.userDone

			consoleSummary.appendUsersProgressBar(counters)

			consoleSummary.toString must beEqualTo("Users  : [#########-] 90%" + END_OF_LINE)
		}
	}
}
