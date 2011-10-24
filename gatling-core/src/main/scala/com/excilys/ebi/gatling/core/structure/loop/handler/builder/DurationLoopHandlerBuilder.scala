package com.excilys.ebi.gatling.core.structure.loop.handler.builder
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder._
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.structure.builder.AbstractStructureBuilder
import com.excilys.ebi.gatling.core.structure.builder.ChainBuilder

class DurationLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, durationValue: Int, durationUnit: TimeUnit)
		extends AbstractLoopHandlerBuilder[B](structureBuilder) {

	def build: B = doBuild(
		List(whileActionBuilder
			.withConditionFunction((c: Context) => c.getWhileDuration <= durationUnit.toMillis(durationValue))
			.withLoopNext(chain)
			.inGroups(structureBuilder.getCurrentGroups)))
}