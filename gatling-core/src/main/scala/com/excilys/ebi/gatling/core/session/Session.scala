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

/**
 * Session class companion
 */
object Session {

	val GATLING_PRIVATE_ATTRIBUTE_PREFIX = "gatling."

	/**
	 * Key for last action duration
	 */
	val LAST_ACTION_DURATION_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "core.lastActionDuration"
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
class Session(val scenarioName: String, val userId: Int, data: Map[String, Any]) {

	def this(scenarioName: String, userId: Int) = this(scenarioName, userId, Map.empty)

	def getAttribute(key: String): Any = getTypedAttribute[Any](key)

	/**
	 * Gets a value from the session
	 *
	 * @param key the key of the requested value
	 * @return the value stored at key
	 */
	def getTypedAttribute[X](key: String): X = data.get(key).getOrElse(throw new IllegalArgumentException("No Matching Session attribute for key " + key)).asInstanceOf[X]

	/**
	 * Gets a value from the session
	 *
	 * This method is to be used only internally, use getAttribute in scenarios
	 *
	 * @param key the key of the requested value
	 * @return the value stored at key as an Option
	 */
	def getAttributeAsOption[T](key: String): Option[T] = data.get(key).asInstanceOf[Option[T]]

	/**
	 * Sets values in the session
	 *
	 * @param attributes map containing several values to be stored in session
	 * @return Nothing
	 */
	def setAttributes(attributes: Map[String, Any]) = new Session(scenarioName, userId, data ++ attributes)

	/**
	 * Sets a single value in the session
	 *
	 * @param attributeKey the key of the attribute
	 * @param attributeValue the value of the attribute
	 * @return Unit
	 */
	def setAttribute(attributeKey: String, attributeValue: Any) = new Session(scenarioName, userId, data + (attributeKey -> attributeValue))

	/**
	 * Removes an attribute and its value from the session
	 *
	 * @param attributeKey the key of the attribute to be removed
	 */
	def removeAttribute(attributeKey: String) = new Session(scenarioName, userId, data - attributeKey)

	def contains(attributeKey: String) = data.contains(attributeKey)

	/**
	 * Gets the last action duration
	 *
	 * @return last action duration in milliseconds
	 */
	private[gatling] def getLastActionDuration: Long = getAttributeAsOption[Long](Session.LAST_ACTION_DURATION_KEY).getOrElse(0L)

	override def toString = new StringBuilder().append("scenarioName='").append(scenarioName).append("' userId='").append(userId).append("' data='").append(data).append("'").toString
}
