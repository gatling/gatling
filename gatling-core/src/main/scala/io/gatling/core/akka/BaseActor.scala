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
package io.gatling.core.akka

import scala.concurrent.duration.Duration

import akka.actor.{ Actor, Terminated }
import com.typesafe.scalalogging.LazyLogging

abstract class BaseActor extends Actor with LazyLogging {

  implicit def system = context.system
  def scheduler = system.scheduler
  implicit def dispatcher = system.dispatcher

  // FIXME is ReceiveTimeout set up by default?
  override def preStart(): Unit = context.setReceiveTimeout(Duration.Undefined)

  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    logger.error(s"Actor $this crashed on message $message", reason)

  override def unhandled(message: Any): Unit =
    message match {
      case Terminated(dead) => super.unhandled(message)
      case unknown          => throw new IllegalArgumentException(s"Actor $this doesn't support message $unknown")
    }
}
