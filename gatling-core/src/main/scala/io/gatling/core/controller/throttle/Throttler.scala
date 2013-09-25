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
package io.gatling.core.controller.throttle

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.config.Protocol
import io.gatling.core.util.TimeHelper.nowMillis

case class ThrottlingProtocol(limit: Long => Int) extends Protocol

class ThisSecondThrottler(val limit: Int, var count: Int = 0) {

	def increment { count += 1 }
	def limitNotReached = count < limit
}

class Throttler(globalProfile: Option[ThrottlingProtocol], scenarioProfiles: Map[String, ThrottlingProtocol]) extends AkkaDefaults {

	val buffer = collection.mutable.Queue.empty[(String, () => Unit)]

	var thisTickStart: Long = _
	var thisTickGlobalThrottler: Option[ThisSecondThrottler] = _
	var thisTickPerScenarioThrottlers: Map[String, ThisSecondThrottler] = _
	var requestPeriod: Double = _
	var thisTickRequestCount: Int = _

	newSecond()

	private def newSecond() {
		thisTickStart = nowMillis
		thisTickGlobalThrottler = globalProfile.map(p => new ThisSecondThrottler(p.limit(thisTickStart)))
		thisTickPerScenarioThrottlers = scenarioProfiles.mapValues(p => new ThisSecondThrottler(p.limit(thisTickStart)))
		val globalLimit = thisTickGlobalThrottler.map(_.limit)
		val perScenarioLimits = thisTickPerScenarioThrottlers.map(_._2.limit)
		val maxNumberOfRequests =
			if (perScenarioLimits.isEmpty)
				globalLimit.get
			else
				math.max(perScenarioLimits.max, globalLimit.getOrElse(0))

		requestPeriod = 1000.0 / maxNumberOfRequests
		thisTickRequestCount = 0
	}

	private def throttle(scenarioName: String, request: () => Unit, shift: Int) = {
		val scenarioThrottler = thisTickPerScenarioThrottlers.get(scenarioName)

		val sending = thisTickGlobalThrottler.map(_.limitNotReached).getOrElse(true) && scenarioThrottler.map(_.limitNotReached).getOrElse(true)
		if (sending) {
			thisTickGlobalThrottler.foreach(_.increment)
			scenarioThrottler.foreach(_.increment)
			val delay = math.max(0, (requestPeriod * thisTickRequestCount).toInt - shift)
			thisTickRequestCount += 1

			if (delay == 0) {
				request()
			} else {
				scheduler.scheduleOnce(delay milliseconds) {
					request()
				}
			}
		}

		sending
	}

	def send(scenarioName: String, request: () => Unit) {
		if (!throttle(scenarioName, request, (nowMillis - thisTickStart).toInt))
			buffer += scenarioName -> request
	}

	def flushBuffer() {
		newSecond()
		// FIXME ugly, side effecting, can do better?
		// + no need to keep on testing when global limit is reached
		buffer.dequeueAll { case (scenarioName, request) => throttle(scenarioName, request, 0) }
	}
}
