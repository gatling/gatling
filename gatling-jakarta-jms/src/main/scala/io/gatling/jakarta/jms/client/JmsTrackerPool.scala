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

package io.gatling.jakarta.jms.client

import java.util.concurrent.ConcurrentHashMap

import io.gatling.commons.util.Clock
import io.gatling.core.actor.{ ActorRef, ActorSystem }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.jakarta.jms.action.JmsLogging
import io.gatling.jakarta.jms.protocol.JmsMessageMatcher

import io.netty.util.concurrent.DefaultThreadFactory
import jakarta.jms.Destination

object JmsTrackerPool {
  private val JmsConsumerThreadFactory = new DefaultThreadFactory("gatling-jms-consumer")
}

final class JmsTrackerPool(
    sessionPool: JmsSessionPool,
    system: ActorSystem,
    statsEngine: StatsEngine,
    clock: Clock,
    configuration: GatlingConfiguration
) extends JmsLogging
    with NameGen {
  private val trackers = new ConcurrentHashMap[(Destination, Option[String]), ActorRef[JmsTracker.Command]]

  def tracker(destination: Destination, selector: Option[String], listenerThreadCount: Int, messageMatcher: JmsMessageMatcher): ActorRef[JmsTracker.Command] =
    trackers.computeIfAbsent(
      (destination, selector),
      _ => {
        val tracker = system.actorOf(JmsTracker.actor(genName("jmsTrackerActor"), statsEngine, clock, configuration))

        for (_ <- 1 to listenerThreadCount) {
          // jms session pool logic creates a session per thread and stores it in thread local.
          // After that the thread can be thrown away. The jms provider takes care of receiving and dispatching
          val thread = JmsTrackerPool.JmsConsumerThreadFactory.newThread { () =>
            val consumer = sessionPool.jmsSession().createConsumer(destination, selector.orNull)
            consumer.setMessageListener { message =>
              val matchId = messageMatcher.responseMatchId(message)
              logMessage(s"Message received JMSMessageID=${message.getJMSMessageID} matchId=$matchId", message)
              tracker ! JmsTracker.Command.MessageReceived(matchId, clock.nowMillis, message)
            }
          }

          thread.start()
        }

        tracker
      }
    )
}
