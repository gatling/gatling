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

@RunWith(classOf[JUnitRunner])
class ConsoleDataWriterSpec extends Specification {

	"console summary progress bar" should {

		"handle it correctly when all the users are waiting" in {
			val buff = new StringBuilder
			ConsoleSummary.appendUsersProgressBar(buff, new UserCounters(11))

			buff.toString must beEqualTo("Users  : [                                                                 ]  0%\n")
		}

		"handle it correctly when all the users are running" in {
			val buff = new StringBuilder
			val counters = new UserCounters(11)
			for (i <- 1 to 11) counters.userStart

			ConsoleSummary.appendUsersProgressBar(buff, counters)

			buff.toString must beEqualTo("Users  : [-----------------------------------------------------------------]  0%\n")
		}

		"handle it correctly when all the users are done" in {
			val buff = new StringBuilder
			val counters = new UserCounters(11)
			for (i <- 1 to 11) counters.userStart
			for (i <- 1 to 11) counters.userDone

			ConsoleSummary.appendUsersProgressBar(buff, counters)

			buff.toString must beEqualTo("Users  : [#################################################################]100%\n")
		}

		"handle it correctly when there are running and done users" in {
			val buff = new StringBuilder
			val counters = new UserCounters(11)
			for (i <- 1 to 11) counters.userStart
			for (i <- 1 to 10) counters.userDone

			ConsoleSummary.appendUsersProgressBar(buff, counters)

			buff.toString must beEqualTo("Users  : [###########################################################------] 90%\n")
		}
	}
}
