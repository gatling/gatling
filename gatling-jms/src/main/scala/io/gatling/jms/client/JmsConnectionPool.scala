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

package io.gatling.jms.client

import java.util.concurrent.ConcurrentHashMap
import javax.jms.ConnectionFactory

import scala.jdk.CollectionConverters._

import io.gatling.commons.model.Credentials
import io.gatling.commons.util.Clock
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.StatsEngine

import akka.actor.ActorSystem

class JmsConnectionPool(system: ActorSystem, statsEngine: StatsEngine, clock: Clock, configuration: GatlingConfiguration) {

  private val connections = new ConcurrentHashMap[ConnectionFactory, JmsConnection]

  def jmsConnection(connectionFactory: ConnectionFactory, credentials: Option[Credentials]): JmsConnection = {
    val connection = connections.computeIfAbsent(
      connectionFactory,
      (connectionFactory: ConnectionFactory) => {
        val connection = credentials match {
          case Some(Credentials(username, password)) => connectionFactory.createConnection(username, password)
          case _                                     => connectionFactory.createConnection()
        }
        connection.start()
        new JmsConnection(connection, credentials, system, statsEngine, clock, configuration)
      }
    )

    if (connection.credentials != credentials) {
      throw new UnsupportedOperationException("The same ConnectionFactory was already used to create a connection with different credentials")
    }

    connection
  }

  def close(): Unit = connections.values().asScala.foreach(_.close())
}
