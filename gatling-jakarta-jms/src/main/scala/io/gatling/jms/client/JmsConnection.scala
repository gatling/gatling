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

package io.gatling.jms.client

import java.util.concurrent.ConcurrentHashMap

import io.gatling.commons.model.Credentials
import io.gatling.commons.util.Clock
import io.gatling.core.actor.{ ActorRef, ActorSystem }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine
import io.gatling.jms.protocol.JmsMessageMatcher
import io.gatling.jms.request._

import jakarta.jms.{ Connection, Destination }

final class JmsConnection(
    connection: Connection,
    val credentials: Option[Credentials],
    system: ActorSystem,
    statsEngine: StatsEngine,
    clock: Clock,
    configuration: GatlingConfiguration
) {
  private val sessionPool = new JmsSessionPool(connection)

  private val staticQueues = new ConcurrentHashMap[String, Destination]
  private val staticTopics = new ConcurrentHashMap[String, Destination]

  def destination(jmsDestination: JmsDestination): Expression[Destination] = {
    val jmsSession = sessionPool.jmsSession()
    jmsDestination match {
      case JmsDestination.TemporaryQueue => jmsSession.createTemporaryQueue().expressionSuccess
      case JmsDestination.TemporaryTopic => jmsSession.createTemporaryTopic().expressionSuccess
      case JmsDestination.Queue(name)    => name.map(n => staticQueues.computeIfAbsent(n, jmsSession.createQueue _))
      case JmsDestination.Topic(name)    => name.map(n => staticTopics.computeIfAbsent(n, jmsSession.createTopic _))
    }
  }

  private val producerPool = new JmsProducerPool(sessionPool)

  def producer(destination: Destination, deliveryMode: Int): JmsProducer =
    producerPool.producer(destination, deliveryMode)

  private val trackerPool = new JmsTrackerPool(sessionPool, system, statsEngine, clock, configuration)

  def tracker(destination: Destination, selector: Option[String], listenerThreadCount: Int, messageMatcher: JmsMessageMatcher): ActorRef[JmsTracker.Command] =
    trackerPool.tracker(destination, selector, listenerThreadCount, messageMatcher)

  def close(): Unit = {
    producerPool.close()
    sessionPool.close()
    connection.close()
  }
}
