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
  def props[T](feeder: Feeder[T], controller: ActorRef): Props = Props(new FeedActor(feeder, controller))
}

private final class FeedActor[T](val feeder: Feeder[T], controller: ActorRef) extends BaseActor {

  private def withErrorHandling(f: => Validation[Record[Any]]): Validation[Record[Any]] =
    try {
      f
    } catch {
      case _: NoSuchElementException | _: ArrayIndexOutOfBoundsException => "Feeder is now empty, stopping engine".failure
      case NonFatal(e) =>
        logger.error("Feeder crashed", e)
        s"Feeder crashed: ${e.detailedMessage}".failure
    }

  private def pollSingleRecord(): Validation[Record[Any]] =
    withErrorHandling(feeder.next().success)

  private def pollMultipleRecords(n: Int): Validation[Record[Any]] =
    withErrorHandling {
      if (n <= 0) {
        s"$n is not a valid number of records".failure
      } else {
        val map = feeder
          .next()
          .view
          .mapValues { value =>
            val array = new Array[Any](n)
            array(0) = value
            array
          }
          .toMap

        for {
          n <- 2 to n
          (key, value) <- feeder.next()
        } map.get(key).foreach(array => array(n - 1) = value)

        map.success
      }
    }

  def receive: Receive = { case FeedMessage(session, number, next) =>
    val newAttributes = number match {
      case Some(n) => pollMultipleRecords(n)
      case _       => pollSingleRecord()
    }

    newAttributes match {
      case Success(attr)    => next ! session.setAll(attr)
      case Failure(message) => controller ! ControllerCommand.Crash(new IllegalStateException(message))
    }
  }

  override def postStop(): Unit =
    feeder match {
      case closeable: AutoCloseable => closeable.close()
      case _                        =>
    }
}

final case class FeedMessage(session: Session, num: Option[Int], next: Action)
