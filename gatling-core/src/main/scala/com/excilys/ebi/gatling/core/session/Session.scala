/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.session

import scala.collection.immutable.{ Map => IMap }
import scala.collection.mutable.Map

import com.excilys.ebi.gatling.core.config.ProtocolConfiguration
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

import akka.actor.Uuid

/**
 * Session class companion
 */
object Session {
	/**
	 * Key for last action duration
	 */
	val LAST_ACTION_DURATION_KEY = "gatling.core.lastActionDuration"
	/**
	 * Key for protocol configurations
	 */
	val PROTOCOL_CONFIGURATIONS_KEY = "gatling.core.protocolConfigurations"
}
/**
 * Session class representing the session passing through a scenario for a given user
 *
 * This session stores all needed data between requests
 *
 * @constructor creates a new session
 * @param scenarioName the name of the current scenario
 * @param userId the id of the current user
 * @param writeActorUuid the uuid of the actor responsible for logging
 * @param data the map that stores all values needed
 */
class Session(val scenarioName: String, val userId: Int, var data: Map[String, Any]) extends Logging {

	def this(scenarioName: String, userId: Int) = this(scenarioName, userId, Map.empty)

	/**
	 * Gets a value from the session
	 *
	 * @param key the key of the requested value
	 * @return the value stored at key, StringUtils.EMPTY if it does not exist
	 */
	def getAttribute(key: String): Any = {
		assert(!key.startsWith("gatling."), "keys starting with gatling. are reserved for internal purpose. If using this method internally please use getAttributeAsOption instead")

		val result = data.get(key).getOrElse {
			logger.warn("No Matching Attribute for key: '{}' in session", key)
			EMPTY
		}
		logger.debug("[Session] found '{}' at '{}'", result, key)
		result
	}

	/**
	 * Gets a value from the session
	 *
	 * This method is to be used only internally, use getAttribute in scenarios
	 *
	 * @param key the key of the requested value
	 * @return the value stored at key as an Option
	 */
	private[gatling] def getAttributeAsOption[TYPE](key: String): Option[TYPE] = {
		assert(key.startsWith("gatling."), "This method should not be used with keys that are not reserved, ie: starting with gatling.")
		data.get(key).asInstanceOf[Option[TYPE]]
	}

	/**
	 * Sets values in the session
	 *
	 * @param attributes map containing several values to be stored in session
	 * @return Nothing
	 */
	def setAttributes(attributes: scala.Predef.Map[String, Any]) = data ++= attributes

	/**
	 * Sets a single value in the session
	 *
	 * @param attributeKey the key of the attribute
	 * @param attributeValue the value of the attribute
	 * @return Unit
	 */
	def setAttribute(attributeKey: String, attributeValue: Any): Unit = data += (attributeKey -> attributeValue)

	/**
	 * Removes an attribute and its value from the session
	 *
	 * @param attributeKey the key of the attribute to be removed
	 */
	def removeAttribute(attributeKey: String) = data -= attributeKey

	/**
	 * Gets the last action duration
	 *
	 * @return last action duration in milliseconds
	 */
	private[gatling] def getLastActionDuration: Long = getAttributeAsOption[Long](Session.LAST_ACTION_DURATION_KEY).getOrElse(0L)

	/**
	 * Gets a protocol configuration based on its type
	 *
	 * @param protocolType the type of the protocol as a string
	 * @return the protocol configuration requested
	 */
	private[gatling] def getProtocolConfiguration(protocolType: String) = {
		getAttributeAsOption[IMap[String, ProtocolConfiguration]](Session.PROTOCOL_CONFIGURATIONS_KEY).map {
			_.get(protocolType)
		}.getOrElse(throw new UnsupportedOperationException("The protocol configuration map does not exist."))
	}

	/**
	 * Sets the protocol configuration map in the session
	 *
	 * @param configurations the sequence containing all the protocol configurations
	 */
	private[gatling] def setProtocolConfig(configurations: Seq[ProtocolConfiguration]) = {
		val configSeq = for (config <- configurations) yield (config.getProtocolType -> config)

		setAttribute(Session.PROTOCOL_CONFIGURATIONS_KEY, configSeq.toMap[String, ProtocolConfiguration])
	}
}
