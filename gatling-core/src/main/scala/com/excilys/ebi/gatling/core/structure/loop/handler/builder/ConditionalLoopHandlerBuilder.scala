package com.excilys.ebi.gatling.core.structure.loop.handler.builder
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder._
import com.excilys.ebi.gatling.core.structure.builder.AbstractStructureBuilder
import com.excilys.ebi.gatling.core.structure.builder.ChainBuilder

class ConditionalLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, conditionFunction: Context => Boolean, counterName: Option[String])
		extends AbstractLoopHandlerBuilder[B](structureBuilder) {
	def build: B = doBuild(List(whileActionBuilder withConditionFunction conditionFunction withLoopNext chain inGroups structureBuilder.getCurrentGroups withCounterName counterName))
}