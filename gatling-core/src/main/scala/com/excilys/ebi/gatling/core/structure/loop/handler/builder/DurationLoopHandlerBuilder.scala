package com.excilys.ebi.gatling.core.structure.loop.handler.builder
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder._
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.structure.builder.AbstractStructureBuilder
import com.excilys.ebi.gatling.core.structure.builder.ChainBuilder
import akka.actor.TypedActor
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.context.handler.TimerBasedIterationHandler._

class DurationLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, durationValue: Int, durationUnit: TimeUnit, counterName: Option[String])
		extends AbstractLoopHandlerBuilder[B](structureBuilder) {

	def build: B = {
		doBuild(
			List(whileActionBuilder
				.withConditionFunction((c: Context, a: Action) => (System.currentTimeMillis - getTimerValue(c, a.getUuidAsString)) <= durationUnit.toMillis(durationValue))
				.withLoopNext(chain)
				.inGroups(structureBuilder.getCurrentGroups)
				.withCounterName(counterName)))
	}
}