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

package io.gatling.jms.action

import javax.jms.Message

import io.gatling.commons.validation._
import io.gatling.core.action.RequestAction
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.session._
import io.gatling.core.util.NameGen
import io.gatling.jms.client.{ JmsConnection, JmsConnectionPool, JmsProducer }
import io.gatling.jms.protocol.JmsProtocol
import io.gatling.jms.request._

class Around(before: () => Unit, after: () => Unit) {

  def apply(f: => Any): Unit = {
    before()
    f
    after()
  }
}

abstract class JmsAction(
    attributes: JmsAttributes,
    protocol: JmsProtocol,
    pool: JmsConnectionPool,
    throttler: Option[Throttler]
) extends RequestAction
    with JmsLogging
    with NameGen {

  override val requestName: Expression[String] = attributes.requestName

  protected val jmsConnection: JmsConnection = pool.jmsConnection(protocol.connectionFactory, protocol.credentials)
  private val jmsDestination = jmsConnection.destination(attributes.destination)

  override def sendRequest(requestName: String, session: Session): Validation[Unit] =
    for {
      jmsType <- resolveOptionalExpression(attributes.jmsType, session)
      props <- resolveProperties(attributes.messageProperties, session)
      resolvedJmsDestination <- jmsDestination(session)
      JmsProducer(jmsSession, producer) = jmsConnection.producer(resolvedJmsDestination, protocol.deliveryMode)
      message <- attributes.message.jmsMessage(session, jmsSession)
      around <- aroundSend(requestName, session, message)
    } yield {
      props.foreach { case (key, value) => message.setObjectProperty(key, value) }
      jmsType.foreach(message.setJMSType)

      throttler match {
        case Some(th) => th.throttle(session.scenario, () => around(producer.send(message)))
        case _        => around(producer.send(message))
      }
    }

  private def resolveProperties(
      properties: Map[Expression[String], Expression[Any]],
      session: Session
  ): Validation[Map[String, Any]] =
    properties.foldLeft(Map.empty[String, Any].success) { case (resolvedProperties, (key, value)) =>
      for {
        key <- key(session)
        value <- value(session)
        resolvedProperties <- resolvedProperties
      } yield resolvedProperties + (key -> value)
    }

  protected def aroundSend(requestName: String, session: Session, message: Message): Validation[Around]
}
