/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.context

import akka.actor.Uuid
import com.excilys.ebi.gatling.core.log.Logging
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.TimeUnit

/**
 * Companion object of Context class
 */
object Context {
	/**
	 * Key for last action duration
	 * This duration is to be substracted to next pause duration
	 */
	val LAST_ACTION_DURATION_ATTR_NAME = "gatling.core.lastActionDuration"
	/**
	 * Key for while duration
	 * This duration is updated each time the while loop has been executed
	 */
	val WHILE_DURATION = "gatling.core.whileDurationElapsed"
	/**
	 * Key for last while access
	 * When a loop has been executed, this value is updated
	 */
	val LAST_WHILE_ACCESS = "gatling.core.lastWhileAccess"
}
/**
 * Context class represent the context passing through a scenario for a given user
 *
 * This context stores all needed data between requests
 *
 * @constructor creates a new context
 * @param scenarioName the name of the current scenario
 * @param userId the id of the current user
 * @param writeActorUuid the uuid of the actor responsible for logging
 * @param data the map that stores all values needed
 */
class Context(val scenarioName: String, val userId: Int, val writeActorUuid: Uuid, var data: Map[String, Any]) extends Logging {

	/**
	 * @return the current user id
	 */
	def getUserId = userId

	/**
	 * @return the uuid of the actor responsible for logging
	 */
	def getWriteActorUuid = writeActorUuid

	/**
	 * @return the scenario name
	 */
	def getScenarioName = scenarioName

	/**
	 * @return the map containing all values
	 */
	def getData = data

	/**
	 * Gets a value from the context
	 *
	 * @param key the key of the requested value
	 * @return the value stored at key, StringUtils.EMPTY if it does not exist
	 */
	def getAttribute(key: String): Any = {
		if (key.startsWith("gatling."))
			throw new UnsupportedOperationException("keys starting with gatling. are reserved for internal purpose. If using this method internally please use getAttributeAsOption instead")
		val result = data.get(key).getOrElse {
			logger.warn("No Matching Attribute for key: '{}' in context", key)
			StringUtils.EMPTY
		}
		logger.debug("[Context] found '{}' at '{}'", result, key)
		result
	}

	/**
	 * Gets a value from the context
	 *
	 * This method is to be used only internally, prefer using getAttribute in scenarios
	 *
	 * @param key the key of the requested value
	 * @return the value stored at key as an Option
	 */
	def getAttributeAsOption(key: String): Option[Any] = {
		if (key.startsWith("gatling."))
			data.get(key)
		else
			throw new UnsupportedOperationException("This method should not be used with keys that are not reserved, ie: starting with gatling.")
	}

	/**
	 * Sets values in the context
	 *
	 * @param attributes map containing several values to be stored in context
	 * @return Nothing
	 */
	def setAttributes(attributes: Map[String, Any]) = {
		data ++= attributes
	}

	/**
	 * Sets a single value in the context
	 *
	 * @param attributeKey the key of the attribute
	 * @param attributeValue the value of the attribute
	 * @return Nothing
	 */
	def setAttribute(attributeKey: String, attributeValue: Any) = {
		data += (attributeKey -> attributeValue)
	}

	/**
	 * Gets the last action duration
	 *
	 * @return last action duration in nanoseconds
	 */
	def getLastActionDuration: Long =
		data.get(Context.LAST_ACTION_DURATION_ATTR_NAME).map { value =>
			value match {
				case s: String => s.toLong
			}
		}.getOrElse(0L)

	/**
	 * Gets the duration of current while loop
	 *
	 * @return the number of milliseconds elapsed from the beginning of the while loop
	 */
	def getWhileDuration: Long = {
		val currentValue = data.get(Context.WHILE_DURATION).map { value =>
			value match {
				case s: String => s.toLong
			}
		}.getOrElse(0L)
		logger.debug("Current Duration Value: {}", currentValue)

		val nowMillis = System.currentTimeMillis

		if (currentValue == 0) {
			setLastWhileAccess(nowMillis)
			setWhileDuration(1L)
			1L
		} else {
			val newValue = currentValue + nowMillis - getLastWhileAccess
			setWhileDuration(newValue)
			setLastWhileAccess(nowMillis)
			logger.debug("New Duration Value: {}", newValue)
			newValue
		}
	}

	/**
	 * Sets the value of while duration to 0 milliseconds
	 *
	 * This method must be called at the end of the while loop
	 *
	 * @return Nothing
	 */
	def resetWhileDuration = setWhileDuration(0L)

	/**
	 * Sets the value of while duration
	 *
	 * @param value the value to be set in milliseconds
	 */
	private def setWhileDuration(value: Long) = {
		data += (Context.WHILE_DURATION -> value.toString)
	}

	/**
	 * Sets the value of last while access
	 *
	 * @param value the value to be set in milliseconds
	 */
	private def setLastWhileAccess(value: Long) = {
		data += (Context.LAST_WHILE_ACCESS -> value.toString)
	}

	/**
	 * Gets the value of last while access
	 *
	 * @return the value of last while access in milliseconds
	 */
	private def getLastWhileAccess = {
		data.get(Context.LAST_WHILE_ACCESS).map { value =>
			value match {
				case s: String => s.toLong
			}
		}.getOrElse(0L)
	}
}
