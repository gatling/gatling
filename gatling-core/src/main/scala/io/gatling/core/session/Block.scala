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
package io.gatling.core.session

import io.gatling.core.result.message.{ OK, KO, Status }
import akka.actor.ActorRef
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success }
import com.typesafe.scalalogging.slf4j.StrictLogging

sealed trait Block

sealed trait CounterBlock extends Block {
  def counterName: String
}

object LoopBlock extends StrictLogging {

  def unapply(block: Block): Option[String] = block match {
    case ExitASAPLoopBlock(counterName, _, _) => Some(counterName)
    case ExitOnCompleteLoopBlock(counterName) => Some(counterName)
    case _                                    => None
  }

  def continue(continueCondition: Expression[Boolean], session: Session): Boolean = continueCondition(session) match {
    case Success(eval) => eval
    case Failure(message) =>
      logger.error(s"Condition evaluation crashed with message '$message', exiting loop")
      false
  }
}

case class ExitOnCompleteLoopBlock(counterName: String) extends CounterBlock

case class ExitASAPLoopBlock(counterName: String, condition: Expression[Boolean], loopActor: ActorRef) extends CounterBlock

case class TryMaxBlock(counterName: String, tryMaxActor: ActorRef, status: Status = OK) extends CounterBlock

case class GroupBlock(name: String, groupHierarchy: List[String], startDate: Long = nowMillis, cumulatedResponseTime: Long = 0L, oks: Int = 0, kos: Int = 0) extends Block {
  def status: Status = if (kos > 0) KO else OK
}
