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
package io.gatling.core.action

import scala.annotation.tailrec

import akka.actor.ActorRef
import io.gatling.core.result.message.KO
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.{ GroupStackEntry, Session }
import io.gatling.core.util.TimeHelper.nowMillis

class ExitHereIfFailed(val next: ActorRef) extends Chainable with DataWriterClient {

  def execute(session: Session) {

    val now = nowMillis

      @tailrec
      def failAllPendingGroups(stack: List[GroupStackEntry]) {
        stack match {
          case Nil =>
          case head :: tail =>
            writeGroupData(session, stack, head.startDate, now, KO)
            failAllPendingGroups(tail)
        }
      }

    if (session.status == KO) {
      failAllPendingGroups(session.groupStack)
      UserEnd.instance ! session
    } else next ! session
  }
}
