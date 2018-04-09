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

package io.gatling.jms.protocol

import javax.jms.{ ConnectionFactory, DeliveryMode }

import io.gatling.commons.model.Credentials

/**
 * JmsProtocolBuilder allows building of the JMS protocol
 * <p>
 * This allows multiple scenarios or jms methods to refer to a single protocol configuration.
 * <p>
 * See your JMS provider documentation for information on the values to set here.
 */
case object JmsProtocolBuilderBase {

  def connectionFactory(cf: ConnectionFactory) = JmsProtocolBuilder(cf)
}

case class JmsProtocolBuilder(
    connectionFactory:    ConnectionFactory,
    creds:                Option[Credentials] = None,
    deliveryMode:         Int                 = DeliveryMode.NON_PERSISTENT,
    messageMatcher:       JmsMessageMatcher   = MessageIDMessageMatcher,
    listenerThreadCount:  Option[Int]         = None,
    replyTimeout:         Option[Long]        = None
) {

  def credentials(user: String, password: String) = copy(creds = Some(Credentials(user, password)))
  def usePersistentDeliveryMode = copy(deliveryMode = DeliveryMode.PERSISTENT)
  def useNonPersistentDeliveryMode = copy(deliveryMode = DeliveryMode.NON_PERSISTENT)
  def matchByMessageID = messageMatcher(MessageIDMessageMatcher)
  def matchByCorrelationID = messageMatcher(CorrelationIDMessageMatcher)
  def messageMatcher(matcher: JmsMessageMatcher) = copy(messageMatcher = matcher)
  @deprecated("noop, replaced with replyTimeout which is per request, will be removed in 3.0", "3.0.0-M5")
  def receiveTimeout(timeout: Long): JmsProtocolBuilder = this
  def replyTimeout(timeout: Long): JmsProtocolBuilder = copy(replyTimeout = Some(timeout))
  def listenerThreadCount(threadCount: Int): JmsProtocolBuilder = copy(listenerThreadCount = Some(threadCount))

  def build = JmsProtocol(
    credentials = creds,
    deliveryMode = deliveryMode,
    messageMatcher = messageMatcher,
    replyTimeout = replyTimeout,
    listenerThreadCount = listenerThreadCount,
    connectionFactory = connectionFactory
  )
}
