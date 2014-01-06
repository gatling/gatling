/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.jms

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.ProtocolRegistry
import akka.actor.ActorRef
import io.gatling.core.akka.AkkaDefaults
import akka.actor.Props
import java.io.{ Serializable => JSerializable }
import io.gatling.jms._

case class JmsRequestBuilderBase(requestName: String) {

	def reqreply = JmsRequestBuilderQueue(requestName, JmsReqReplyActionBuilder)
}

case class JmsRequestBuilderQueue(requestName: String, factory: JmsAttributes => ActionBuilder) {

	def queue(queueName: String) = JmsRequestBuilderMessage(requestName, queueName, factory)
}

case class JmsRequestBuilderMessage(requestName: String, queueName: String, factory: JmsAttributes => ActionBuilder) {

	def textMessage(text: String) = message(TextJmsMessage(text))
	def bytesMessage(bytes: Array[Byte]) = message(BytesJmsMessage(bytes))
	def mapMessage(map: Map[String, Object]) = message(MapJmsMessage(map))
	def objectMessage(o: JSerializable) = message(ObjectJmsMessage(o))
	private def message(mess: JmsMessage) = JmsRequestBuilder(JmsAttributes(requestName, queueName, mess), factory)
}

case class JmsRequestBuilder(attributes: JmsAttributes, factory: JmsAttributes => ActionBuilder) {

	/**
	 * Add JMS message properties (aka headers) to the outbound message
	 */
	def addProperty(key: String, value: Any) = new JmsRequestBuilder(attributes.copy(messageProperties = attributes.messageProperties + (key -> value)), factory)

	/**
	 * Add a check that will be perfomed on each received JMS response message before giving Gatling on OK/KO response
	 */
	def check(checks: JmsCheck*) = new JmsRequestBuilder(attributes.copy(checks = attributes.checks ::: checks.toList), factory)

	def build(): ActionBuilder = factory(attributes)
}
