package com.excilys.ebi.gatling.core.action.builder
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.context.handler.CounterBasedIterationHandler
import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder._

object IterationStep extends Enumeration {
	type IterationStep = Value
	val INIT, INCREMENT, EXPIRE = Value
}

import IterationStep._

object CountBasedIterationActionBuilder extends CounterBasedIterationHandler {
	def initCounterAction(counterName: String) = initClass(counterName, INIT)
	def incrementCounterAction(counterName: String) = initClass(counterName, INCREMENT)
	def expireCounterAction(counterName: String) = initClass(counterName, EXPIRE)

	private def initClass(counterName: String, iterationStep: IterationStep) = {
		val contextModifier =
			iterationStep match {
				case INIT => (c: Context, a: Action) => init(c, a.getUuidAsString, Some(counterName))
				case INCREMENT => (c: Context, a: Action) => increment(c, a.getUuidAsString, Some(counterName))
				case EXPIRE => (c: Context, a: Action) => expire(c, a.getUuidAsString, Some(counterName))
			}
		simpleActionBuilder(contextModifier)
	}
}
