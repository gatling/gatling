/**
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
package com.excilys.ebi.gatling.core.context.handler

import scala.collection.immutable.Stack
import com.excilys.ebi.gatling.core.context.Context

object CounterBasedIterationHandler {

	/**
	 * Key prefix for Counters
	 */
	val COUNTER_KEY_PREFIX = "gatling.core.counter."

	def getCounterValue(context: Context, counterName: String) = {
		context.getAttributeAsOption(COUNTER_KEY_PREFIX + counterName).getOrElse(throw new IllegalAccessError("Counter does not exist, check the name of the key " + counterName)).asInstanceOf[Int]
	}
}

import CounterBasedIterationHandler._

trait CounterBasedIterationHandler extends IterationHandler {

	abstract override def init(context: Context, uuid: String, userDefinedName: Option[String]) = {
		super.init(context, uuid, userDefinedName)
		val counterName = userDefinedName.getOrElse(uuid)
		context.getAttributeAsOption(COUNTER_KEY_PREFIX + counterName).getOrElse {
			context.setAttribute(COUNTER_KEY_PREFIX + counterName, -1)
		}
	}

	abstract override def increment(context: Context, uuid: String, userDefinedName: Option[String]) = {
		super.increment(context, uuid, userDefinedName)
		val key = COUNTER_KEY_PREFIX + userDefinedName.getOrElse(uuid)
		val currentValue: Int = context.getAttributeAsOption(key).getOrElse(throw new IllegalAccessError("You must call startCounter before this method is called")).asInstanceOf[Int]

		context.setAttribute(key, currentValue + 1)
	}

	abstract override def expire(context: Context, uuid: String, userDefinedName: Option[String]) = {
		super.expire(context, uuid, userDefinedName)
		context.removeAttribute(COUNTER_KEY_PREFIX + userDefinedName.getOrElse(uuid))
	}
}