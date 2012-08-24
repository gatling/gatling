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
import scala.collection.mutable.Map
import scala.math.ceil

@RunWith(classOf[JUnitRunner])
class ConsoleDataWriterSpec extends Specification {

	val totalUsers = 11
	val dummyRequestCounters = Map(("request",RequestCounters(0,0)))
	val progressBarIndex = 3
	val prelude = "Users  : ["

	"console summary progress bar" should {

		"handle it correctly when all the users are waiting" in {
			val userCounters = new UserCounters(totalUsers)
			val consoleSummary = ConsoleSummary(0,Map(("Scenario",userCounters)),dummyRequestCounters)
			val progressBar = consoleSummary.toString.split("\\n")(progressBarIndex)
			val ending = "]  0%"
			val progressBarSize = ConsoleSummary.outputLength -prelude.length - ending.length
			val allUsersWaiting = " " * progressBarSize
			progressBar must beEqualTo(prelude + allUsersWaiting + ending)
		}

		"handle it correctly when all the users are running" in {
			val userCounters = new UserCounters(totalUsers)
			for (i <- 1 to totalUsers) userCounters.userStart
			val consoleSummary = ConsoleSummary(0,Map(("Scenario",userCounters)),dummyRequestCounters)
			val progressBar = consoleSummary.toString.split("\\n")(progressBarIndex)
			val ending = "]  0%"
			val progressBarSize = ConsoleSummary.outputLength -prelude.length - ending.length
			val allUsersRunning = "-" * progressBarSize
				progressBar must beEqualTo(prelude + allUsersRunning + ending)
		}

		"handle it correctly when all the users are done" in {
			val userCounters = new UserCounters(totalUsers)
			for (i <- 1 to totalUsers) userCounters.userStart
			for (i <- 1 to totalUsers) userCounters.userDone
			val consoleSummary = ConsoleSummary(0,Map(("Scenario",userCounters)),dummyRequestCounters)
			val progressBar = consoleSummary.toString.split("\\n")(progressBarIndex)
			val ending = "]100%"
			val progressBarSize = ConsoleSummary.outputLength -prelude.length - ending.length
			val allUsersDone = "#" * progressBarSize
			progressBar must beEqualTo(prelude + allUsersDone + ending)
		}

		"handle it correctly when there are running and done users" in {
			val userCounters = new UserCounters(totalUsers)
			val userDone = totalUsers - 1
			for (i <- 1 to totalUsers) userCounters.userStart
			for (i <- 1 to userDone) userCounters.userDone
			val consoleSummary = ConsoleSummary(0,Map(("Scenario",userCounters)),dummyRequestCounters)
			val progressBar = consoleSummary.toString.split("\\n")(progressBarIndex)
			val ending = "] 90%"
			val progressBarSize = ConsoleSummary.outputLength -prelude.length - ending.length
			val NinetyPercentUsersDone = "#" * ((userDone / totalUsers.toDouble) * progressBarSize).toInt
			val TenPercentUsersDone = "-" * ceil(((1 - userDone / totalUsers.toDouble) * progressBarSize)).toInt
			progressBar must beEqualTo(prelude + NinetyPercentUsersDone + TenPercentUsersDone + ending)
		}
	}
}
