/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import scala.annotation.switch
import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._
import io.gatling.core.akka.BaseActor
import io.gatling.core.controller.ControllerCommand
import io.gatling.core.feeder.{ Feeder, Record }
import io.gatling.core.session.Session

import akka.actor.{ ActorRef, Props }

object FeedActor {
  def props[T](feeder: Feeder[T], controller: ActorRef): Props = Props(new FeedActor(feeder, controller))
}

class FeedActor[T](val feeder: Feeder[T], controller: ActorRef) extends BaseActor {

  private def pollNewAttributes(numberOfRecords: Int): Validation[Record[T]] =
    try {
      (numberOfRecords: @switch) match {
        case 1 =>
          feeder.next().success
        case n if n > 0 =>
          (1 to n)
            .foldLeft(Map.empty[String, T]) { (acc, i) =>
              feeder.next().foldLeft(acc) { case (acc2, (key, value)) =>
                acc2 + (key + Integer.toString(i) -> value)
              }
            }
            .success
        case _ => s"$numberOfRecords is not a valid number of records".failure
      }
    } catch {
      case _: NoSuchElementException => "Feeder is now empty, stopping engine".failure
      case NonFatal(e)               => s"Feeder crashed: ${e.detailedMessage}".failure
    }

  def receive: Receive = { case FeedMessage(session, number, next) =>
    pollNewAttributes(number) match {
      case Success(newAttributes) => next ! session.setAll(newAttributes)
      case Failure(message)       => controller ! ControllerCommand.Crash(new IllegalStateException(message))
    }
  }

  override def postStop(): Unit =
    feeder match {
      case closeable: AutoCloseable => closeable.close()
      case _                        =>
    }
}

final case class FeedMessage(session: Session, number: Int, next: Action)
