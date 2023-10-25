/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import java.{ util => ju }

import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._
import io.gatling.core.akka.BaseActor
import io.gatling.core.controller.ControllerCommand
import io.gatling.core.feeder.{ Feeder, Record }
import io.gatling.core.session.Session

import akka.actor.{ ActorRef, Props }
import io.github.metarank.cfor._

private[core] object FeedActor {
  def props[T](feeder: Feeder[T], feederName: Option[String], generateJavaCollection: Boolean, controller: ActorRef): Props = Props(
    new FeedActor(feeder, feederName, generateJavaCollection, controller)
  )
}

private final class FeedActor[T](val feeder: Feeder[T], feederName: Option[String], generateJavaCollection: Boolean, controller: ActorRef) extends BaseActor {
  private def emptyFeederFailure = s"Feeder ${feederName.getOrElse("unknown")} is now empty, stopping engine".failure

  private def pollSingleRecord(): Validation[Record[Any]] =
    if (feeder.hasNext) {
      feeder.next().success
    } else {
      emptyFeederFailure
    }

  private def toJavaValues(array: Array[Record[Any]], n: Int, key: String): ju.List[Any] = {
    val values = new ju.ArrayList[Any](n)
    cfor(0 until n) { j =>
      values.add(array(j)(key))
    }
    values
  }

  private def toScalaValues(array: Array[Record[Any]], n: Int, key: String): Seq[Any] = {
    val values = new Array[Any](n)
    cfor(0 until n) { j =>
      values(j) = array(j)(key)
    }
    values.toSeq
  }

  private def pollMultipleRecords(n: Int): Validation[Record[Any]] = {
    val array = new Array[Record[Any]](n)
    var i = 0
    while (feeder.hasNext && i < n) {
      array(i) = feeder.next()
      i += 1
    }

    if (i == n) {
      array(0).keys
        .map { key =>
          val values = if (generateJavaCollection) toJavaValues(array, n, key) else toScalaValues(array, n, key)
          key -> values
        }
        .toMap
        .success

    } else {
      emptyFeederFailure
    }
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
