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

import io.gatling.core.config.Protocol

/**
 * Wraps a JMS protocol configuration
 * @author jasonk@bluedevel.com
 */
object JmsProtocol {
	val default = JmsProtocol(
		contextFactory = None,
		connectionFactoryName = None,
		jmsUrl = None,
		username = None,
		password = None,
		listenerCount = 1,
		deliveryMode = javax.jms.DeliveryMode.NON_PERSISTENT)
}

/**
 * Wraps a JMS protocol configuration
 */
case class JmsProtocol(
	contextFactory: Option[String],
	connectionFactoryName: Option[String],
	jmsUrl: Option[String],
	username: Option[String],
	password: Option[String],
	listenerCount: Int,
	deliveryMode: Int) extends Protocol

