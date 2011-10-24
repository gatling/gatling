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
		context.unsetAttribute(COUNTER_KEY_PREFIX + userDefinedName.getOrElse(uuid))
	}

}