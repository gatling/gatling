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
import io.gatling.core.controller.Controller
import io.gatling.core.controller.inject.InjectionProfile
import io.gatling.core.result.message.Start
import io.gatling.core.result.writer.UserMessage
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.zeroMs

case class Scenario(name: String, entryPoint: ActorRef, injectionProfile: InjectionProfile) extends AkkaDefaults {

  def run(userIdRoot: String, offset: Int) {

      def startUser(i: Int) {
        val session = Session(name, userIdRoot + (i + offset))
        Controller ! UserMessage(session.scenarioName, session.userId, Start, session.startDate, 0L)
        entryPoint ! session
      }

    injectionProfile.allUsers.zipWithIndex.foreach {
      case (startingTime, index) =>
        if (startingTime == zeroMs)
          startUser(index)
        else
          scheduler.scheduleOnce(startingTime) {
            startUser(index)
          }
    }
  }
}
