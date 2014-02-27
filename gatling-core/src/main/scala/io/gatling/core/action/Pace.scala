/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.action

import scala.concurrent.duration.{ Duration, DurationLong }

import akka.actor.ActorRef
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.TimeHelper.nowMillis

/**
/**
 * Pace provides a means to limit the frequency with which an action is run, by specifying a minimum wait time between.
 * 
 * Originally contributed by James Pickering.
 */
 * iterations.
 *
 * @param intervalExpr a function that decides how long to wait before the next iteration
 * @param counter the name of the counter used to keep track of the run state. Typically this would be random, but
 *                can be set explicitly if needed
 * @param next the next actor in the chain
 */
class Pace(intervalExpr: Expression[Duration], counter: String, val next: ActorRef) extends Interruptable with Failable {
	/**
	 * Pace keeps track of when it can next run using a counter in the session. If this counter does not exist, it will
	 * run immediately. On each run, it increments the counter by intervalExpr.
	 *
	 * @param session the session of the virtual user
	 * @return Nothing
	 */
	override def executeOrFail(session: Session) = {
		intervalExpr(session) map { interval =>
			val startTimeOpt = session(counter).asOption[Long]
			val startTime = startTimeOpt.getOrElse(nowMillis)
			val nextStartTime = startTime + interval.toMillis
			val waitTime = startTime - nowMillis
			def doNext = next ! session.set(counter, nextStartTime)
			if (waitTime > 0) {
				scheduler.scheduleOnce(waitTime milliseconds)(doNext)
			} else {
				if (startTimeOpt.isDefined) logger.info(s"Previous run overran by ${-waitTime}ms. Running immediately")
				doNext
			}
		}
	}
}
