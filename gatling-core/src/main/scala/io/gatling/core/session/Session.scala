/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import io.gatling.core.result.message.GroupStackEntry
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.util.TypeHelper
import io.gatling.core.validation.{ FailureWrapper, Validation }
import io.gatling.core.result.message.OK
import io.gatling.core.result.message.KO

/**
 * Private Gatling Session attributes
 */
object SessionPrivateAttributes {

	val privateAttributePrefix = "gatling."
	val timeShift = privateAttributePrefix + "core.timeShift"
	val failed = privateAttributePrefix + "core.failed"
	val mustExitOnFail = privateAttributePrefix + "core.mustExitOnFailed"
	val startDate = privateAttributePrefix + "core.startDate"
	val groupStack = privateAttributePrefix + "core.groupStack"
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
	def get[T](key: String): Option[T] = attributes.get(key).map(_.asInstanceOf[T])
	def get[T](key: String, default: => T): T = attributes.get(key).map(_.asInstanceOf[T]).getOrElse(default)
	def getV[T: ClassTag](key: String): Validation[T] = attributes.get(key).map(TypeHelper.as[T](_)).getOrElse(undefinedSessionAttributeMessage(key).failure[T])
	def set(newAttributes: Map[String, Any]) = copy(attributes = attributes ++ newAttributes)
	def set(key: String, value: Any) = copy(attributes = attributes + (key -> value))
	def remove(key: String) = if (contains(key)) copy(attributes = attributes - key) else this
	def contains(attributeKey: String) = attributes.contains(attributeKey)

	def setFailed: Session = {
		val failedSession = set(SessionPrivateAttributes.failed, java.lang.Boolean.TRUE.toString)
		groupStack match {
			case head :: tail if (head.status == OK) => failedSession.set(SessionPrivateAttributes.groupStack, groupStack.map(_.copy(status = KO))) // fail all the groups
			case _ => failedSession
		}
	}
	def clearFailed: Session = remove(SessionPrivateAttributes.failed)
	def isFailed: Boolean = contains(SessionPrivateAttributes.failed)
	def setMustExitOnFail: Session = set(SessionPrivateAttributes.mustExitOnFail, java.lang.Boolean.TRUE.toString)
	def isMustExitOnFail: Boolean = contains(SessionPrivateAttributes.mustExitOnFail)
	def clearMustExitOnFail: Session = remove(SessionPrivateAttributes.mustExitOnFail)
	def shouldExitBecauseFailed: Boolean = isFailed && isMustExitOnFail

	private[gatling] def setTimeShift(timeShift: Long): Session = set(SessionPrivateAttributes.timeShift, timeShift)
	private[gatling] def increaseTimeShift(time: Long): Session = setTimeShift(time + getTimeShift)
	private[gatling] def getTimeShift: Long = get[Long](SessionPrivateAttributes.timeShift, 0L)

	private[gatling] def start = set(SessionPrivateAttributes.startDate, nowMillis)
	private[gatling] def startDate = get[Long](SessionPrivateAttributes.startDate, Long.MinValue)

	private[gatling] def groupStack: List[GroupStackEntry] = get[List[GroupStackEntry]](SessionPrivateAttributes.groupStack, Nil)
	private[gatling] def enterGroup(groupName: String): Session = set(SessionPrivateAttributes.groupStack, GroupStackEntry(groupName, nowMillis, OK) :: groupStack)
	private[gatling] def exitGroup: Session = groupStack match {
		case Nil => this
		case group :: Nil => remove(SessionPrivateAttributes.groupStack)
		case head :: tail => set(SessionPrivateAttributes.groupStack, tail)
	}
}
