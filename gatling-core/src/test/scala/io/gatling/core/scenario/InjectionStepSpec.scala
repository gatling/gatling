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
package io.gatling.core.scenario
import scala.concurrent.duration._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class InjectionStepSpec extends Specification {

	"RampInjection" should {
		val ramp = RampInjection(5, 1 second)

		"return the correct number of users" in {
			ramp.users must beEqualTo(5)
		}

		"return the correct injection duration" in {
			ramp.duration must beEqualTo(1 second)
		}

		val scheduling = ramp.chain(Iterator.empty).toList

		"schedule with a correct interval" in {
			val interval0 = scheduling(1) - scheduling(0)
			val interval1 = scheduling(2) - scheduling(1)
			scheduling.length must beEqualTo(ramp.users) and (interval0 must beEqualTo(interval1)) and (interval0 must beEqualTo(250 milliseconds))
		}

		"the first and the last users should be correctly scheduled" in {
			val first = scheduling.head
			val last = scheduling.last
			first must beEqualTo(0 second) and (last must beEqualTo(1 second))
		}
	}

	"NothingForInjection" should {
		val wait = NothingForInjection(1 second)

		"return the correct number of users" in {
			wait.users must beEqualTo(0)
		}

		"return the correct injection duration" in {
			wait.duration must beEqualTo(1 second)
		}

		"return the correct injection scheduling" in {
			wait.chain(Iterator.empty) must beEmpty
		}
	}

	"AtOnceInjection" should {
		val peak = AtOnceInjection(4)

		"return the correct number of users" in {
			peak.users must beEqualTo(4)
		}

		val scheduling = peak.chain(Iterator.empty).toList

		"return the correct injection duration" in {
			scheduling.max must beEqualTo(0 second)
		}

		"return the correct injection scheduling" in {
			val uniqueScheduling = scheduling.toSet
			uniqueScheduling must contain(0 second) and (scheduling must have length (peak.users))
		}
	}

	"RampRateInjection" should {
		val rampRate = RampRateInjection(2, 4, 10 seconds)

		"return the correct injection duration" in {
			rampRate.duration must beEqualTo(10 seconds)
		}

		"return the correct number of users" in {
			rampRate.users must beEqualTo(30)
		}

		val scheduling = rampRate.chain(Iterator.empty).toList

		"provides an injection scheduling with the correct number of elements" in {
			scheduling.length must beEqualTo(rampRate.users)
		}

		"provides an injection scheduling with the correct values" in {
			scheduling(0) must beEqualTo(0 seconds) and (scheduling(1) must beEqualTo(488 milliseconds))
		}
	}

	"SplitInjection" should {

		"provide an appropriate injection scheduling and ignore extra users" in {
			val scheduling = SplitInjection(10, RampInjection(3, 2 seconds), NothingForInjection(5 seconds)).chain(Iterator.empty).toList
			scheduling must beEqualTo(List(0 second, 1 second, 2 seconds, 7 seconds, 8 seconds, 9 seconds, 14 seconds, 15 seconds, 16 seconds))
		}

		"should schedule the first and last user thru the 'into' injection step" in {
			val scheduling = SplitInjection(5, RampInjection(3, 2 seconds), AtOnceInjection(1)).chain(AtOnceInjection(1).chain(Iterator.empty)).toList
			scheduling must beEqualTo(List(0 second, 1 second, 2 seconds, 2 seconds))
		}
	}

	"HeavisideInjection" should {
		val scheduling = HeavisideInjection(100, 5 seconds).chain(Iterator.empty).toList

		"provide an appropriate number of users" in {
			scheduling.length must beEqualTo(100)
		}

		"be of an appropriate duration" in {
			scheduling.last must beEqualTo(5 seconds)
		}

		"provide correct values" in {
			scheduling(1) must beEqualTo(292 milliseconds)
		}

		"have most of the scheduling values close to half of the duration" in {
			val l = scheduling.filter((t) => (t > (1.5 seconds)) && (t < (3.5 seconds))).length
			l must beEqualTo(66)
		}
	}

	// Deactivate Specs2 implicit to be able to use the ones provided in scala.concurrent.duration
	override def intToRichLong(v: Int) = super.intToRichLong(v)
}