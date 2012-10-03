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

import com.excilys.ebi.gatling.core.session.handler.{ CounterBasedIterationHandler, TimerBasedIterationHandler }
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

import grizzled.slf4j.Logging

/**
 * Session class companion
 */
object Session extends Logging {

	val GATLING_PRIVATE_ATTRIBUTE_PREFIX = "gatling."

	val TIME_SHIFT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "core.timeShift"

	val FAILED_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "core.failed"

	val MUST_EXIT_ON_FAIL_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "core.mustExitOnFailed"

	def attributeAsEvaluatableString(key: String): EvaluatableString = (session: Session) => session.getAttributeAsOption[Any](key)
		.map(_.toString)
		.getOrElse {
			warn("Couldn't resolve session attribute " + key)
			EMPTY
		}

	def attributeAsEvaluatableStringSeq(key: String): EvaluatableStringSeq = (session: Session) => session.getAttributeAsOption[Any](key)
		.map(value => value match {
			case seq: Seq[_] => seq.map(_.toString)
			case mono => List(mono.toString)
		}).getOrElse {
			warn("Couldn't resolve session attribute " + key)
			List(EMPTY)
		}

	def evaluatableStringToEvaluatableStringSeq(evaluatableString: EvaluatableString): EvaluatableStringSeq = (session: Session) => List(evaluatableString(session))
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
class Session(val scenarioName: String, val userId: Int, data: Map[String, Any] = Map.empty) {

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
	def removeAttribute(attributeKey: String) = if (isAttributeDefined(attributeKey)) new Session(scenarioName, userId, data - attributeKey) else this

	def isAttributeDefined(attributeKey: String) = data.contains(attributeKey)

	/**
	 * This method gets the specified counter from the session
	 *
	 * @param counterName the name of the counter
	 * @return the value of the counter as an integer
	 */
	def getCounterValue(counterName: String) = getAttributeAsOption[Int](CounterBasedIterationHandler.getCounterAttributeName(counterName)).getOrElse(throw new IllegalAccessError("Counter does not exist, check the name of the key " + counterName))

	/**
	 * This method gets the specified timer from the session
	 *
	 * @param timerName the name of the timer
	 * @return the value of the timer as a long
	 */
	def getTimerValue(timerName: String) = getAttributeAsOption[Long](TimerBasedIterationHandler.getTimerAttributeName(timerName)).getOrElse(throw new IllegalAccessError("Timer is not set : " + timerName))

	def setFailed: Session = setAttribute(Session.FAILED_KEY, EMPTY)

	def clearFailed: Session = removeAttribute(Session.FAILED_KEY)

	def isFailed: Boolean = isAttributeDefined(Session.FAILED_KEY)

	def setMustExitOnFail: Session = setAttribute(Session.MUST_EXIT_ON_FAIL_KEY, EMPTY)

	def isMustExitOnFail: Boolean = isAttributeDefined(Session.MUST_EXIT_ON_FAIL_KEY)

	def clearMustExitOnFail: Session = removeAttribute(Session.MUST_EXIT_ON_FAIL_KEY)

	def shouldExitBecauseFailed: Boolean = isFailed && isMustExitOnFail

	private[gatling] def setTimeShift(timeShift: Long): Session = setAttribute(Session.TIME_SHIFT_KEY, timeShift)

	private[gatling] def increaseTimeShift(time: Long): Session = setTimeShift(time + getTimeShift)

	private[gatling] def getTimeShift: Long = getAttributeAsOption[Long](Session.TIME_SHIFT_KEY).getOrElse(0L)

	override def toString = "scenarioName='" + scenarioName + "' userId='" + userId + "' data='" + data + "'"
}
