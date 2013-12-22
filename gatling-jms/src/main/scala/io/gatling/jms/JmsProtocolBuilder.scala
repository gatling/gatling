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

/**
 * JmsProtocolBuilder
 * @author jasonk@bluedevel.com
 */
object JmsProtocolBuilder {
	val default = new JmsProtocolBuilder(JmsProtocol.default)
}

/**
 * JmsProtocolBuilder allows building of the JMS protocol
 * <p>
 * This allows multiple scenarios or jms methods to refer to a single protocol configuration.
 * <p>
 * See your JMS provider documentation for information on the values to set here.
 */
case class JmsProtocolBuilder(protocol: JmsProtocol) {

	/**
	 * Configures the JMS Connection factory name - see your JMS provider docs
	 */
	def connectionFactoryName(cf: String) = copy(protocol = protocol.copy(connectionFactoryName = Some(cf)))

	/**
	 * Configures the JMS URL - see your JMS provider docs
	 */
	def url(theUrl: String) = copy(protocol = protocol.copy(jmsUrl = Some(theUrl)))

	/**
	 * Configures the JMS connection credentials - see your JMS provider docs
	 */
	def credentials(user: String, pass: String) = copy(protocol = protocol.copy(username = Some(user), password = Some(pass)))

	/**
	 * Configures the context factory name - see your JMS provider docs
	 */
	def contextFactory(factory: String) = copy(protocol = protocol.copy(contextFactory = Some(factory)))

	/**
	 * A number (default=1) of listener threads will be set up to wait for JMS response messages.
	 * <p>
	 * In extremely high volume testing you may need to set up multiple listener threads.
	 */
	def listenerCount(count: Int) = copy(protocol = protocol.copy(listenerCount = count))

	/**
	 * Configure the JMS [[javax.jms.DeliveryMode]].
	 */
	def deliveryMode(mode: Int) = copy(protocol = protocol.copy(deliveryMode = mode))

	/**
	 * Builds the required protocol. Generally only used by Gatling.
	 */
	def build = {
		require(!protocol.connectionFactoryName.isEmpty, "Connection factory must be set")
		require(!protocol.jmsUrl.isEmpty, "JMS URL must be set")
		require(!protocol.contextFactory.isEmpty, "Context Factory must be set")
		require(protocol.listenerCount >= 1, "JMS response listener count must be at least 1")
		require(protocol.username.isEmpty == protocol.password.isEmpty, "Username or password should both be set or neither")
		require((protocol.deliveryMode == javax.jms.DeliveryMode.PERSISTENT)
			|| (protocol.deliveryMode == javax.jms.DeliveryMode.NON_PERSISTENT),
			"DeliveryMode must be set to either PERSISTENT or NON_PERSISTENT as per JMS API specs.")
		protocol
	}

}
