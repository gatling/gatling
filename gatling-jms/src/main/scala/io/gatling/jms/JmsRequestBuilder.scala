/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.io.{ Serializable => JSerializable }

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session._

case class JmsRequestBuilderBase(requestName: String) {

	def reqreply = JmsRequestBuilderQueue(requestName, JmsReqReplyActionBuilder)
}

case class JmsRequestBuilderQueue(requestName: String, factory: JmsAttributes => ActionBuilder) {

	def queue(queueName: String) = JmsRequestBuilderMessage(requestName, queueName, factory)
}

case class JmsRequestBuilderMessage(requestName: String, queueName: String, factory: JmsAttributes => ActionBuilder) {

	def textMessage(text: Expression[String]) = message(TextJmsMessage(text))
	def bytesMessage(bytes: Expression[Array[Byte]]) = message(BytesJmsMessage(bytes))
	def mapMessage(map: Map[String, Any]): JmsRequestBuilder = mapMessage(map.expression)
	def mapMessage(map: Expression[Map[String, Any]]): JmsRequestBuilder = message(MapJmsMessage(map))
	def objectMessage(o: Expression[JSerializable]) = message(ObjectJmsMessage(o))
	private def message(mess: JmsMessage) = JmsRequestBuilder(JmsAttributes(requestName, queueName, mess), factory)
}

case class JmsRequestBuilder(attributes: JmsAttributes, factory: JmsAttributes => ActionBuilder) {

	/**
	 * Add JMS message properties (aka headers) to the outbound message
	 */
	def property(key: String, value: Any) = new JmsRequestBuilder(attributes.copy(messageProperties = attributes.messageProperties + (key -> value)), factory)

	/**
	 * Add a check that will be perfomed on each received JMS response message before giving Gatling on OK/KO response
	 */
	def check(checks: JmsCheck*) = new JmsRequestBuilder(attributes.copy(checks = attributes.checks ::: checks.toList), factory)

	def build(): ActionBuilder = factory(attributes)
}
