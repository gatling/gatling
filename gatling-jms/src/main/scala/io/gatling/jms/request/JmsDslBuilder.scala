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

package io.gatling.jms.request

import java.io.{ Serializable => JSerializable }

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Expression, ExpressionSuccessWrapper }
import io.gatling.jms.JmsCheck
import io.gatling.jms.action.{ RequestReplyBuilder, SendBuilder }

import com.softwaremill.quicklens._

case class JmsDslBuilderBase(requestName: Expression[String]) {

  def send(implicit configuration: GatlingConfiguration) = SendDslBuilderQueue(requestName, configuration)

  @deprecated("Use requestReply, will be removed in 3.0.0", "3.0.0-M5")
  def reqreply(implicit configuration: GatlingConfiguration) = requestReply

  def requestReply(implicit configuration: GatlingConfiguration) = RequestReplyDslBuilderQueue(requestName, configuration)
}

case class SendDslBuilderQueue(
    requestName:   Expression[String],
    configuration: GatlingConfiguration
) {

  def queue(name: String) = destination(JmsQueue(name))

  def destination(destination: JmsDestination) = SendDslDslBuilderMessage(requestName, destination, configuration)
}

case class RequestReplyDslBuilderQueue(
    requestName:   Expression[String],
    configuration: GatlingConfiguration
) {

  def queue(name: String) = destination(JmsQueue(name))

  def destination(destination: JmsDestination) = RequestReplyDslBuilderMessage(requestName, destination, JmsTemporaryQueue, None, configuration)
}

case class SendDslDslBuilderMessage(
    requestName:   Expression[String],
    destination:   JmsDestination,
    configuration: GatlingConfiguration
) {

  def textMessage(text: Expression[String]) = message(TextJmsMessage(text))
  def bytesMessage(bytes: Expression[Array[Byte]]) = message(BytesJmsMessage(bytes))
  def objectMessage(o: Expression[JSerializable]) = message(ObjectJmsMessage(o))

  private def message(mess: JmsMessage) =
    SendDslBuilder(JmsAttributes(requestName, destination, None, mess), SendBuilder.apply(_, configuration))
}

case class RequestReplyDslBuilderMessage(
    requestName:     Expression[String],
    destination:     JmsDestination,
    replyDest:       JmsDestination,
    messageSelector: Option[String],
    configuration:   GatlingConfiguration
) {
  /**
   * Add a reply queue, if not specified dynamic queue is used
   */
  def replyQueue(name: String) = replyDestination(JmsQueue(name))
  def replyDestination(destination: JmsDestination) = this.copy(replyDest = destination)

  /**
   * defines selector for reply destination that is used for responses
   */
  def selector(selector: String) = this.copy(messageSelector = Some(selector))

  def textMessage(text: Expression[String]) = message(TextJmsMessage(text))
  def bytesMessage(bytes: Expression[Array[Byte]]) = message(BytesJmsMessage(bytes))
  def mapMessage(map: Map[String, Any]): RequestReplyDslBuilder = mapMessage(map.expressionSuccess)
  def mapMessage(map: Expression[Map[String, Any]]): RequestReplyDslBuilder = message(MapJmsMessage(map))
  def objectMessage(o: Expression[JSerializable]) = message(ObjectJmsMessage(o))

  private def message(mess: JmsMessage) =
    RequestReplyDslBuilder(JmsAttributes(requestName, destination, messageSelector, mess), RequestReplyBuilder.apply(_, replyDest, configuration))
}

case class SendDslBuilder(attributes: JmsAttributes, factory: JmsAttributes => ActionBuilder) {

  /**
   * Add JMS message properties (aka headers) to the outbound message
   */
  def property(key: Expression[String], value: Expression[Any]) = this.modify(_.attributes.messageProperties).using(_ + (key -> value))

  def jmsType(jmsType: Expression[String]) = this.modify(_.attributes.jmsType).setTo(Some(jmsType))

  def build(): ActionBuilder = factory(attributes)
}

case class RequestReplyDslBuilder(attributes: JmsAttributes, factory: JmsAttributes => ActionBuilder) {

  /**
   * Add JMS message properties (aka headers) to the outbound message
   */
  def property(key: Expression[String], value: Expression[Any]) = this.modify(_.attributes.messageProperties).using(_ + (key -> value))

  def jmsType(jmsType: Expression[String]) = this.modify(_.attributes.jmsType).setTo(Some(jmsType))

  /**
   * Add a check that will be performed on each received JMS response message before giving Gatling on OK/KO response
   */
  def check(checks: JmsCheck*) = this.modify(_.attributes.checks).using(_ ::: checks.toList)

  def build(): ActionBuilder = factory(attributes)
}
