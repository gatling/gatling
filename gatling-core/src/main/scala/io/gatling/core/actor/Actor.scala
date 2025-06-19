/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.core.actor

import com.typesafe.scalalogging.StrictLogging

import java.util.concurrent.atomic.AtomicLong

abstract class Actor[Message](val name: String) extends StrictLogging {

  private var schedulerRef: Option[Scheduler] = None
  private var selfRef: Option[ActorRef[Message]] = None

  private[actor] def initRefs(schedulerRef: Scheduler, selfRef: ActorRef[Message]): Unit = {
    this.schedulerRef = Some(schedulerRef)
    this.selfRef = Some(selfRef)
  }

  protected def scheduler: Scheduler = schedulerRef.getOrElse(throw new IllegalStateException("Can't access Scheduler before initialization"))
  protected def self: ActorRef[Message] = selfRef.getOrElse(throw new IllegalStateException("Can't access self before initialization"))

  final def become(newBehavior: Behavior[Message]): Effect[Message] = _ => newBehavior

  final val stay: Effect[Message] = identity

  final val die: Effect[Message] = {
    val droppedMsgCount = new AtomicLong()
    become { msg =>
      if (droppedMsgCount.incrementAndGet() <= 20) {
        logger.info(s"Dropping msg '$msg' as actor is dead")
      }
      stay
    }
  }

  def dropUnexpected(msg: Message): Effect[Message] = {
    logger.info(s"Dropping unexpected msg '$msg'")
    stay
  }

  def dieOnUnexpected(msg: Message): Effect[Message] = {
    logger.info(s"Dying because of unexpected msg '$msg'")
    die
  }

  private[actor] def init(): Behavior[Message]
}
