/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.jms.client

import java.util.concurrent.ConcurrentHashMap
import javax.jms.Destination

import scala.collection.JavaConverters._
import scala.util.Try

import io.gatling.commons.util.ClockSingleton._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.jms.action.JmsLogging
import io.gatling.jms.protocol.JmsMessageMatcher

import akka.actor.ActorSystem

class JmsTrackerPool(sessionPool: JmsSessionPool, system: ActorSystem, statsEngine: StatsEngine, configuration: GatlingConfiguration) extends JmsLogging with NameGen {

  private val trackers = new ConcurrentHashMap[(Destination, Option[String]), JmsTracker]

  def tracker(destination: Destination, selector: Option[String], listenerThreadCount: Option[Int], messageMatcher: JmsMessageMatcher): JmsTracker =
    trackers.computeIfAbsent((destination, selector), _ => {
      val actor = system.actorOf(Tracker.props(statsEngine, configuration), genName("jmsTrackerActor"))

      for (elem <- 1 to listenerThreadCount.getOrElse(1)) {
        // jms session pool logic creates a session per thread and stores it in thread local.
        // After that the thread can be throughen away. The jms provider takes care of receiving and dispatching
        val thread = new Thread(() => {
          val consumer = sessionPool.jmsSession().createConsumer(destination, selector.orNull)
          consumer.setMessageListener(message => {
            val matchId = messageMatcher.responseMatchId(message)
            logMessage(s"Message received JMSMessageID=${message.getJMSMessageID} matchId=$matchId", message)
            actor ! MessageReceived(matchId, nowMillis, message)
          })
        })

        thread.start()
      }

      new JmsTracker(actor)
    })
}
