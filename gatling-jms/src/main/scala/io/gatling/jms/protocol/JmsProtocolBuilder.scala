/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.jms.protocol

import javax.jms.{ ConnectionFactory, DeliveryMode }

import io.gatling.core.config.Credentials

/**
 * JmsProtocolBuilder allows building of the JMS protocol
 * <p>
 * This allows multiple scenarios or jms methods to refer to a single protocol configuration.
 * <p>
 * See your JMS provider documentation for information on the values to set here.
 */
case object JmsProtocolBuilderBase {

  def connectionFactory(cf: ConnectionFactory) = JmsProtocolBuilderListenerCountStep(cf, None)
}

case class JmsProtocolBuilderListenerCountStep(
    connectionFactory: ConnectionFactory,
    credentials:       Option[Credentials]
) {

  def credentials(user: String, password: String) = copy(credentials = Some(Credentials(user, password)))

  def listenerCount(count: Int) = {
    require(count > 0, "JMS response listener count must be at least 1")
    JmsProtocolBuilder(connectionFactory, credentials, count)
  }
}

case class JmsProtocolBuilder(
    connectionFactory: ConnectionFactory,
    credentials:       Option[Credentials],
    listenerCount:     Int,
    deliveryMode:      Int                 = DeliveryMode.NON_PERSISTENT,
    messageMatcher:    JmsMessageMatcher   = MessageIDMessageMatcher,
    receiveTimeout:    Option[Long]        = None
) {

  def usePersistentDeliveryMode = copy(deliveryMode = DeliveryMode.PERSISTENT)
  def useNonPersistentDeliveryMode = copy(deliveryMode = DeliveryMode.NON_PERSISTENT)
  def matchByMessageID = messageMatcher(MessageIDMessageMatcher)
  def matchByCorrelationID = messageMatcher(CorrelationIDMessageMatcher)
  def messageMatcher(matcher: JmsMessageMatcher) = copy(messageMatcher = matcher)
  def receiveTimeout(timeout: Long): JmsProtocolBuilder = copy(receiveTimeout = Some(timeout))

  def build = new JmsProtocol(
    credentials = credentials,
    listenerCount = listenerCount,
    deliveryMode = deliveryMode,
    messageMatcher = messageMatcher,
    receiveTimeout = receiveTimeout,
    connectionFactory = connectionFactory
  )
}
