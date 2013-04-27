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

import io.gatling.core.result.message.{ GroupStackEntry, KO, OK, Status }
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.util.TypeHelper
import io.gatling.core.validation.{ FailureWrapper, Validation }

/**
 * Private Gatling Session attributes
 */
object SessionPrivateAttributes {

	val privateAttributePrefix = "gatling."
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
case class Session(
	scenarioName: String,
	userId: Int,
	attributes: Map[String, Any] = Map.empty,
	startDate: Long = 0L,
	groupStack: List[GroupStackEntry] = Nil,
	statusStack: List[Status] = List(OK),
	timeShift: Long = 0L) extends Logging {

	import SessionPrivateAttributes._

	private[gatling] def start = copy(startDate = nowMillis)

	private[gatling] def enterGroup(groupName: String): Session = copy(groupStack = GroupStackEntry(groupName, nowMillis, OK) :: groupStack)
	private[gatling] def exitGroup: Session = copy(groupStack = groupStack.tail)

	def fail: Session = statusStack match {
		case OK :: tail => copy(statusStack = KO :: tail, groupStack = groupStack.map(_.copy(status = KO))) // fail all the groups
		case _ => this

	}
	private[gatling] def enterTryBlock: Session = copy(statusStack = OK :: statusStack)
	private[gatling] def exitTryBlock: Session = statusStack match {
		case KO :: _ :: tail => copy(statusStack = KO :: tail) // fail upper block only if not failed
		case _ :: tail => copy(statusStack = tail)
	}
	def failedInTryBlock: Boolean = statusStack.size > 1 && statusStack.head == KO
	def status: Status = if (statusStack.contains(KO)) KO else OK

	private[gatling] def setTimeShift(timeShift: Long): Session = copy(timeShift = timeShift)
	private[gatling] def increaseTimeShift(time: Long): Session = copy(timeShift = time + timeShift)

	def apply(name: String) = attributes(name)
	def get[T](key: String): Option[T] = attributes.get(key).map(_.asInstanceOf[T])
	def get[T](key: String, default: => T): T = attributes.get(key).map(_.asInstanceOf[T]).getOrElse(default)
	def getV[T: ClassTag](key: String): Validation[T] = attributes.get(key).map(TypeHelper.as[T](_)).getOrElse(undefinedSessionAttributeMessage(key).failure[T])
	def set(newAttributes: Map[String, Any]) = copy(attributes = attributes ++ newAttributes)
	def set(key: String, value: Any) = copy(attributes = attributes + (key -> value))
	def remove(key: String) = if (contains(key)) copy(attributes = attributes - key) else this
	def contains(attributeKey: String) = attributes.contains(attributeKey)
}
