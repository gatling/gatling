/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.core.action

import io.gatling.commons.validation._
import io.gatling.core.akka.BaseActor
import io.gatling.core.controller.ControllerCommand
import io.gatling.core.feeder.{ Feeder, Record }
import io.gatling.core.session.{ Expression, Session }

import akka.actor.{ Props, ActorRef }

object SingletonFeed {
  def props[T](feeder: Feeder[T]) = Props(new SingletonFeed(feeder))
}

class SingletonFeed[T](val feeder: Feeder[T]) extends BaseActor {

  def receive = {
    case FeedMessage(session, number, controller, next) =>

      def translateRecord(record: Record[T], suffix: Int): Record[T] = record.map { case (key, value) => (key + suffix) -> value }

        def pollRecord(): Validation[Record[T]] =
          if (!feeder.hasNext)
            "Feeder is now empty, stopping engine".failure
          else
            feeder.next().success

        def feedRecords(numberOfRecords: Int): Validation[Session] =
          numberOfRecords match {
            case 1 =>
              pollRecord().map(session.setAll)
            case n if n > 0 =>
              val translatedRecords = Iterator.tabulate(n) { i =>
                pollRecord().map(translateRecord(_, i + 1))
              }.reduce { (record1V, record2V) =>
                for (record1 <- record1V; record2 <- record2V) yield record1 ++ record2
              }
              translatedRecords.map(session.setAll)
            case n => s"$n is not a valid number of records".failure
          }

      val newSession = number(session).flatMap(feedRecords) match {
        case Success(s) => s
        case Failure(message) =>
          logger.error(s"Feed failed: $message, please report.")
          controller ! ControllerCommand.ForceStop(Some(new IllegalStateException(message)))
          session
      }

      next ! newSession
  }
}

case class FeedMessage(session: Session, number: Expression[Int], controller: ActorRef, next: Action)
