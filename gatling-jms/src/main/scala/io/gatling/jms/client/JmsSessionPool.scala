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

import scala.jdk.CollectionConverters._

import jakarta.jms._

final class JmsSessionPool(connection: Connection) {
  private val registeredJmsSessions = ConcurrentHashMap.newKeySet[Session]

  private val jmsSessions = ThreadLocal.withInitial[Session] { () =>
    val s = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    registeredJmsSessions.add(s)
    s
  }

  def jmsSession(): Session = jmsSessions.get()

  def close(): Unit = registeredJmsSessions.asScala.foreach(_.close())
}
