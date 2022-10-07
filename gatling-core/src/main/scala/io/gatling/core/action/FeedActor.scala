/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._
import io.gatling.core.akka.BaseActor
import io.gatling.core.controller.ControllerCommand
import io.gatling.core.feeder.{ Feeder, Record }
import io.gatling.core.session.Session

import akka.actor.{ ActorRef, Props }

private[core] object FeedActor {
  def props[T](feeder: Feeder[T], feederName: Option[String], controller: ActorRef): Props = Props(new FeedActor(feeder, feederName, controller))
}

private final class FeedActor[T](val feeder: Feeder[T], feederName: Option[String], controller: ActorRef) extends BaseActor {
  private def emptyFeederFailure = s"Feeder ${feederName.getOrElse("unknown")} is now empty, stopping engine".failure

  private def pollSingleRecord(): Validation[Record[Any]] =
    if (feeder.hasNext) {
      feeder.next().success
    } else {
      emptyFeederFailure
    }

  private def pollMultipleRecords(n: Int): Validation[Record[Any]] =
    if (feeder.hasNext) {
      val map: Map[String, Array[Any]] = feeder
        .next()
        .view
        .mapValues { value =>
          val array = new Array[Any](n)
          array(0) = value
          array
        }
        .toMap

      var i = 1
      while (feeder.hasNext && i < n) {
        for ((key, value) <- feeder.next()) {
          map.get(key).foreach(array => array(i) = value)
        }
        i += 1
      }

      if (i == n) {
        map.success
      } else {
        emptyFeederFailure
      }
    } else {
      emptyFeederFailure
    }

  def receive: Receive = { case FeedMessage(session, number, next) =>
    try {
      val newAttributes = number match {
        case Some(n) if n > 1  => pollMultipleRecords(n)
        case Some(n) if n <= 0 => s"$n is not a valid number of records".failure
        case _                 => pollSingleRecord()
      }

      newAttributes match {
        case Success(attr)    => next ! session.setAll(attr)
        case Failure(message) => controller ! ControllerCommand.Crash(new IllegalStateException(message))
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"Feeder ${feederName.getOrElse("unknown")} crashed", e)
        s"Feeder ${feederName.getOrElse("unknown")} crashed: ${e.detailedMessage}, stopping engine".failure
    }
  }

  override def postStop(): Unit =
    feeder match {
      case closeable: AutoCloseable => closeable.close()
      case _                        =>
    }
}

final case class FeedMessage(session: Session, num: Option[Int], next: Action)
