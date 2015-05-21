/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.controller

import scala.concurrent.duration._

import akka.actor.{ ActorSystem, ActorRef }

import io.gatling.core.result.message.Start
import io.gatling.core.result.writer.UserMessage
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper._

class BatchScheduler(startTime: Long,
                     batchWindow: FiniteDuration,
                     controller: ActorRef) {

  def scheduleUserStream(system: ActorSystem, userStream: UserStream): Unit = {

    implicit val dispatcher = system.dispatcher

    val scenario = userStream.scenario
    val stream = userStream.stream

      def startUser(i: Long): Unit = {
        val session = Session(scenario = scenario.name,
          userId = i + userStream.offset,
          onExit = scenario.ctx.protocols.onExit)
        controller ! UserMessage(session, Start, 0L)
        scenario.entry ! session
      }

    if (stream.hasNext) {
      val batchTimeOffset = (nowMillis - startTime).millis
      val nextBatchTimeOffset = batchTimeOffset + batchWindow

      var continue = true

      while (stream.hasNext && continue) {

        val (startingTime, index) = stream.next()
        val delay = startingTime - batchTimeOffset
        continue = startingTime < nextBatchTimeOffset

        if (continue && delay <= ZeroMs) {
          startUser(index)
        } else {
          // Reduce the starting time to the millisecond precision to avoid flooding the scheduler
          system.scheduler.scheduleOnce(toMillisPrecision(delay))(startUser(index))
        }
      }

      // schedule next batch
      if (stream.hasNext) {
        system.scheduler.scheduleOnce(batchWindow) {
          controller ! ScheduleNextUserBatch(scenario.name)
        }
      }
    }
  }
}
