/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.core.session

import scala.reflect.ClassTag

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.util.TypeHelper
import io.gatling.core.validation.{ Failure, FailureWrapper, Success, Validation }

/**
 * Private Gatling Session attributes
 */
object SessionPrivateAttributes {

	val privateAttributePrefix = "gatling."

	val timeShift = privateAttributePrefix + "core.timeShift"

	val failed = privateAttributePrefix + "core.failed"

	val mustExitOnFail = privateAttributePrefix + "core.mustExitOnFailed"
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
case class Session(scenarioName: String, userId: Int, attributes: Map[String, Any] = Map.empty) extends Logging {

	def apply(name: String) = attributes(name)

	def get[T: ClassTag](key: String): Option[T] = attributes.get(key).flatMap {
		TypeHelper.as[T](_) match {
			case Success(typedValue) =>
				Some(typedValue)
			case Failure(message) =>
				logger.error(s"Could find value of key $key but wrong type: $message")
				None
		}
	}

	def getV[T: ClassTag](key: String): Validation[T] = attributes.get(key).map(TypeHelper.as[T](_)).getOrElse(undefinedSessionAttributeMessage(key).failure[T])

	def set(newAttributes: Map[String, Any]) = copy(attributes = attributes ++ newAttributes)

	def set(key: String, value: Any) = copy(attributes = attributes + (key -> value))

	def remove(key: String) = if (contains(key)) copy(attributes = attributes - key) else this

	def contains(attributeKey: String) = attributes.contains(attributeKey)

	def setFailed: Session = set(SessionPrivateAttributes.failed, java.lang.Boolean.TRUE.toString)

	def clearFailed: Session = remove(SessionPrivateAttributes.failed)

	def isFailed: Boolean = contains(SessionPrivateAttributes.failed)

	def setMustExitOnFail: Session = set(SessionPrivateAttributes.mustExitOnFail, java.lang.Boolean.TRUE.toString)

	def isMustExitOnFail: Boolean = contains(SessionPrivateAttributes.mustExitOnFail)

	def clearMustExitOnFail: Session = remove(SessionPrivateAttributes.mustExitOnFail)

	def shouldExitBecauseFailed: Boolean = isFailed && isMustExitOnFail

	private[gatling] def setTimeShift(timeShift: Long): Session = set(SessionPrivateAttributes.timeShift, timeShift)

	private[gatling] def increaseTimeShift(time: Long): Session = setTimeShift(time + getTimeShift)

	private[gatling] def getTimeShift: Long = get[Long](SessionPrivateAttributes.timeShift).getOrElse(0L)
}
