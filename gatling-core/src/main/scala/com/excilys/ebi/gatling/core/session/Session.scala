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

import com.excilys.ebi.gatling.core.util.TypeHelper

import grizzled.slf4j.Logging
import scalaz._
import scalaz.Scalaz._

/**
 * Session class companion
 */
object Session extends Logging {

	val GATLING_PRIVATE_ATTRIBUTE_PREFIX = "gatling."

	val TIME_SHIFT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "core.timeShift"

	val FAILED_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "core.failed"

	val MUST_EXIT_ON_FAIL_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "core.mustExitOnFailed"

}

/**
 * Session class representing the session passing through a scenario for a given user
 *
 * This session stores all needed data between requests
 *
 * @constructor creates a new session
 * @param scenarioName the name of the current scenario
 * @param userId the id of the current user
 * @param data the map that stores all values needed
 */
class Session(val scenarioName: String, val userId: Int, attributes: Map[String, Any] = Map.empty) {

	def apply(name: String) = attributes(name)

	def get(name: String): Validation[String, Any] = attributes.get(name).toSuccess(undefinedSessionAttributeMessage(name))

	def getAs[T: ClassManifest](name: String): Validation[String, T] = attributes.get(name).map(TypeHelper.as[T](_)).getOrElse(undefinedSessionAttributeMessage(name).failure[T])

	/**
	 * Sets values in the session
	 *
	 * @param attributes map containing several values to be stored in session
	 * @return Nothing
	 */
	def setAttributes(attributes: Map[String, Any]) = new Session(scenarioName, userId, attributes ++ attributes)

	/**
	 * Sets a single value in the session
	 *
	 * @param attributeKey the key of the attribute
	 * @param attributeValue the value of the attribute
	 * @return Unit
	 */
	def setAttribute(attributeKey: String, attributeValue: Any) = new Session(scenarioName, userId, attributes + (attributeKey -> attributeValue))

	/**
	 * Removes an attribute and its value from the session
	 *
	 * @param attributeKey the key of the attribute to be removed
	 */
	def removeAttribute(attributeKey: String) = if (isAttributeDefined(attributeKey)) new Session(scenarioName, userId, attributes - attributeKey) else this

	def isAttributeDefined(attributeKey: String) = attributes.contains(attributeKey)

	def setFailed: Session = setAttribute(Session.FAILED_KEY, "")

	def clearFailed: Session = removeAttribute(Session.FAILED_KEY)

	def isFailed: Boolean = isAttributeDefined(Session.FAILED_KEY)

	def setMustExitOnFail: Session = setAttribute(Session.MUST_EXIT_ON_FAIL_KEY, "")

	def isMustExitOnFail: Boolean = isAttributeDefined(Session.MUST_EXIT_ON_FAIL_KEY)

	def clearMustExitOnFail: Session = removeAttribute(Session.MUST_EXIT_ON_FAIL_KEY)

	def shouldExitBecauseFailed: Boolean = isFailed && isMustExitOnFail

	private[gatling] def setTimeShift(timeShift: Long): Session = setAttribute(Session.TIME_SHIFT_KEY, timeShift)

	private[gatling] def increaseTimeShift(time: Long): Session = setTimeShift(time + getTimeShift)

	private[gatling] def getTimeShift: Long = getAs[Long](Session.TIME_SHIFT_KEY).getOrElse(0L)

	override def toString = "scenarioName='" + scenarioName + "' userId='" + userId + "' data='" + attributes + "'"
}
