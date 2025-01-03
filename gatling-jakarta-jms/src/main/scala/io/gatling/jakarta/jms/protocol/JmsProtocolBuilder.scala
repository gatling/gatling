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

package io.gatling.jakarta.jms.protocol

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.model.Credentials

import jakarta.jms.{ ConnectionFactory, DeliveryMode }

/**
 * JmsProtocolBuilder allows building of the JMS protocol <p> This allows multiple scenarios or jms methods to refer to a single protocol configuration. <p> See
 * your JMS provider documentation for information on the values to set here.
 */
case object JmsProtocolBuilderBase {
  def connectionFactory(cf: ConnectionFactory): JmsProtocolBuilder = JmsProtocolBuilder(cf, None, DeliveryMode.NON_PERSISTENT, MessageIdMessageMatcher, 1, None)
}

final case class JmsProtocolBuilder(
    connectionFactory: ConnectionFactory,
    creds: Option[Credentials],
    deliveryMode: Int,
    messageMatcher: JmsMessageMatcher,
    listenerThreadCount: Int,
    replyTimeout: Option[FiniteDuration]
) {
  def credentials(user: String, password: String): JmsProtocolBuilder = copy(creds = Some(Credentials(user, password)))
  def usePersistentDeliveryMode: JmsProtocolBuilder = copy(deliveryMode = DeliveryMode.PERSISTENT)
  def useNonPersistentDeliveryMode: JmsProtocolBuilder = copy(deliveryMode = DeliveryMode.NON_PERSISTENT)
  def matchByMessageId: JmsProtocolBuilder = messageMatcher(MessageIdMessageMatcher)
  def matchByCorrelationId: JmsProtocolBuilder = messageMatcher(CorrelationIdMessageMatcher)
  def messageMatcher(matcher: JmsMessageMatcher): JmsProtocolBuilder = copy(messageMatcher = matcher)
  def replyTimeout(timeout: FiniteDuration): JmsProtocolBuilder = copy(replyTimeout = Some(timeout))
  def listenerThreadCount(threadCount: Int): JmsProtocolBuilder = copy(listenerThreadCount = threadCount)

  def build: JmsProtocol = JmsProtocol(
    credentials = creds,
    deliveryMode = deliveryMode,
    messageMatcher = messageMatcher,
    replyTimeout = replyTimeout,
    listenerThreadCount = listenerThreadCount,
    connectionFactory = connectionFactory
  )
}
