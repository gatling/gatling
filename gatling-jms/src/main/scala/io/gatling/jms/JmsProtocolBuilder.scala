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

import io.gatling.core.config.Credentials
import javax.jms.DeliveryMode
import io.gatling.core.config.Credentials

/**
 * JmsProtocolBuilder allows building of the JMS protocol
 * <p>
 * This allows multiple scenarios or jms methods to refer to a single protocol configuration.
 * <p>
 * See your JMS provider documentation for information on the values to set here.
 *
 * @author jasonk@bluedevel.com
 */
case object ConnectionFactoryNameStep {

	def connectionFactoryName(cfn: String) = UrlStep(cfn)
}

case class UrlStep(connectionFactoryName: String) {

	def url(theUrl: String) = ContextFactoryStep(connectionFactoryName, theUrl)
}

case class ContextFactoryStep(connectionFactoryName: String, url: String, credentials: Option[Credentials] = None) {

	def credentials(user: String, password: String) = copy(credentials = Some(Credentials(user, password)))

	def contextFactory(cf: String) = ListenerCountStep(connectionFactoryName, url, credentials, cf)
}

case class ListenerCountStep(connectionFactoryName: String, url: String, credentials: Option[Credentials], contextFactory: String) {

	def listenerCount(count: Int) = {
		require(count > 0, "JMS response listener count must be at least 1")
		JmsProtocolBuilder(connectionFactoryName, url, credentials, contextFactory, count)
	}
}

case class JmsProtocolBuilder(connectionFactoryName: String, url: String, credentials: Option[Credentials], contextFactory: String, listenerCount: Int, deliveryMode: Int = DeliveryMode.NON_PERSISTENT) {

	def usePersistentDeliveryMode() = copy(deliveryMode = DeliveryMode.PERSISTENT)
	def useNonPersistentDeliveryMode() = copy(deliveryMode = DeliveryMode.NON_PERSISTENT)

	def build = new JmsProtocol(
		contextFactory = this.contextFactory,
		connectionFactoryName = this.connectionFactoryName,
		url = this.url,
		credentials = this.credentials,
		listenerCount = this.listenerCount,
		deliveryMode = deliveryMode)
}
