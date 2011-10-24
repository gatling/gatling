package com.excilys.ebi.gatling.core.context.handler
import com.excilys.ebi.gatling.core.context.Context
import scala.collection.immutable.Stack

object TimerBasedIterationHandler {
	/**
	 * Key prefix for Counters
	 */
	val TIMER_KEY_PREFIX = "gatling.core.timer."

	def getTimerValue(context: Context, timerName: String) = {
		context.getAttributeAsOption(TIMER_KEY_PREFIX + timerName).getOrElse(throw new IllegalAccessError("You must call startTimer before this method is called")).asInstanceOf[Long]
	}
}

import TimerBasedIterationHandler._

trait TimerBasedIterationHandler extends IterationHandler {

	abstract override def init(context: Context, uuid: String, userDefinedName: Option[String]) = {
		super.init(context, uuid, userDefinedName)
		context.getAttributeAsOption(TIMER_KEY_PREFIX + uuid).getOrElse {
			context.setAttribute(TIMER_KEY_PREFIX + uuid, System.currentTimeMillis)
		}
	}

	abstract override def increment(context: Context, uuid: String, userDefinedName: Option[String]) = {
		super.increment(context, uuid, userDefinedName)
	}

	abstract override def expire(context: Context, uuid: String, userDefinedName: Option[String]) = {
		super.expire(context, uuid, userDefinedName)
		context.unsetAttribute(TIMER_KEY_PREFIX + uuid)
	}

}