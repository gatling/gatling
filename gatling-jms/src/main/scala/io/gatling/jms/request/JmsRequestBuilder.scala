/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
import io.gatling.core.session._
import io.gatling.jms._
import io.gatling.jms.action.JmsReqReplyActionBuilder

import com.softwaremill.quicklens._

case class JmsRequestBuilderBase(requestName: String) {

  def reqreply(implicit configuration: GatlingConfiguration) = JmsRequestBuilderQueue(requestName, JmsReqReplyActionBuilder.apply)
}

case class JmsRequestBuilderQueue(
    requestName: String,
    factory:     JmsAttributes => ActionBuilder
) {

  def queue(name: String) = destination(JmsQueue(name))

  def destination(destination: JmsDestination) = JmsRequestBuilderMessage(requestName, destination, JmsTemporaryQueue, None, factory)
}

case class JmsRequestBuilderMessage(
    requestName:     String,
    destination:     JmsDestination,
    replyDest:       JmsDestination,
    messageSelector: Option[String],
    factory:         JmsAttributes => ActionBuilder
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
  def mapMessage(map: Map[String, Any]): JmsRequestBuilder = mapMessage(map.expressionSuccess)
  def mapMessage(map: Expression[Map[String, Any]]): JmsRequestBuilder = message(MapJmsMessage(map))
  def objectMessage(o: Expression[JSerializable]) = message(ObjectJmsMessage(o))

  private def message(mess: JmsMessage) =
    JmsRequestBuilder(JmsAttributes(requestName, destination, replyDest, messageSelector, mess), factory)
}

case class JmsRequestBuilder(attributes: JmsAttributes, factory: JmsAttributes => ActionBuilder) {

  /**
   * Add JMS message properties (aka headers) to the outbound message
   */
  def property(key: Expression[String], value: Expression[Any]) = this.modify(_.attributes.messageProperties).using(_ + (key -> value))

  /**
   * Add a check that will be perfomed on each received JMS response message before giving Gatling on OK/KO response
   */
  def check(checks: JmsCheck*) = this.modify(_.attributes.checks).using(_ ::: checks.toList)

  def build(): ActionBuilder = factory(attributes)
}
