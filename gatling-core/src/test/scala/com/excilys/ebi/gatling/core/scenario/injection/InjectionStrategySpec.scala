/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.scenario.injection

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InjectionStrategySpec extends Specification {
	"RampInjection" should {
		val ramp = new RampInjection(5, new FiniteDuration(1l, TimeUnit.SECONDS))

		"return the correct number of users" in {
			ramp.users must beEqualTo(5)
		}

		"return the correct injection duration" in {
			ramp.duration must beEqualTo(new FiniteDuration(1l, TimeUnit.SECONDS))
		}

		val scheduling = ramp.scheduling.toList

		"schedule with a correct interval" in {
			val interval0 = scheduling(1) - scheduling(0)
			val interval1 = scheduling(2) - scheduling(1)
			scheduling.length must beEqualTo(ramp.users) and (interval0 must beEqualTo(interval1)) and (interval0 must beEqualTo(new FiniteDuration(250l, TimeUnit.MILLISECONDS)))
		}

		"the first and the last users should be correctly scheduled" in {
			val first = scheduling.head
			val last = scheduling.last
			first must beEqualTo(new FiniteDuration(0l, TimeUnit.SECONDS)) and (last must beEqualTo(new FiniteDuration(1l, TimeUnit.SECONDS)))
		}
	}

	"WaitInjection" should {
		val wait = new DelayInjection(new FiniteDuration(1l, TimeUnit.SECONDS))

		"return the correct number of users" in {
			wait.users must beEqualTo(0)
		}

		"return the correct injection duration" in {
			wait.duration must beEqualTo(new FiniteDuration(1l, TimeUnit.SECONDS))
		}

		"return the correct injection scheduling" in {
			wait.scheduling must beEmpty
		}
	}

	"PeakInjection" should {
		val peak = new PeakInjection(4)

		"return the correct number of users" in {
			peak.users must beEqualTo(4)
		}

		"return the correct injection duration" in {
			peak.duration must beEqualTo(new FiniteDuration(0l, TimeUnit.SECONDS))
		}

		"return the correct injection scheduling" in {
			val scheduling = peak.scheduling.toList
			val uniqueScheduling = scheduling.toSet
			uniqueScheduling must contain(new FiniteDuration(0l, TimeUnit.SECONDS)) and (scheduling must have length (peak.users))
		}
	}

	"RampRateInjection" should {
		val rampRate = new RampRateInjection(2, 4, new FiniteDuration(10l, TimeUnit.SECONDS))

		"return the correct injection duration" in {
			rampRate.duration must beEqualTo(new FiniteDuration(10l, TimeUnit.SECONDS))
		}

		"return the correct number of users" in {
			rampRate.users must beEqualTo(30)
		}

		val scheduling = rampRate.scheduling.toList

		"provides an injection scheduling with the correct number of elements" in {
			scheduling.length must beEqualTo(rampRate.users)
		}

		"provides an injection scheduling with the correct values" in {
			scheduling(0) must beEqualTo(new FiniteDuration(0l, TimeUnit.SECONDS)) and (scheduling(1) must beEqualTo(new FiniteDuration(488l, TimeUnit.MILLISECONDS)))
		}
	}

}