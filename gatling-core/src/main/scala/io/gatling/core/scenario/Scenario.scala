/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import akka.actor.ActorRef

import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.config.Protocols
import io.gatling.core.controller.Controller
import io.gatling.core.controller.inject.InjectionProfile
import io.gatling.core.result.message.Start
import io.gatling.core.result.writer.UserMessage
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper._

import scala.concurrent.duration._

case class Scenario(name: String, entryPoint: ActorRef, injectionProfile: InjectionProfile, protocols: Protocols) extends AkkaDefaults {

  def run(userIdRoot: String, offset: Int): Unit = {

      def startUser(i: Int): Unit = {
        val session = Session(scenarioName = name,
          userId = userIdRoot + (i + offset),
          userEnd = protocols.userEnd)
        Controller ! UserMessage(session.scenarioName, session.userId, Start, session.startDate, 0L)
        entryPoint ! session
      }

    val start = nowMillis
    val batchWindow = 10 seconds

    val batches = injectionProfile.allUsers.zipWithIndex

      def batchSchedule(): Unit = batches.synchronized {

        if (batches.hasNext) {

          val batchTimeOffset = (nowMillis - start).millis
          val nextBatchTimeOffset = batchTimeOffset + batchWindow

          var continue = true

          while (batches.hasNext && continue) {

            val (startingTime, index) = batches.next

            val delay = startingTime - batchTimeOffset

            continue = startingTime < nextBatchTimeOffset

            if (continue && delay <= ZeroMs)
              startUser(index)

            else
              // Reduce the starting time to the millisecond precision to avoid flooding the scheduler
              scheduler.scheduleOnce(toMillisPrecision(delay)) {
                startUser(index)
              }
          }

          // schedule next batch
          if (batches.hasNext)
            scheduler.scheduleOnce(batchWindow) {
              batchSchedule()
            }
        }
      }

    batchSchedule()
  }
}
