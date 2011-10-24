package com.excilys.ebi.gatling.core.structure.loop.handler.builder
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.action.builder.WhileActionBuilder._
import com.excilys.ebi.gatling.core.structure.builder.AbstractStructureBuilder
import com.excilys.ebi.gatling.core.structure.builder.ChainBuilder

class ConditionalLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, testFunction: Context => Boolean)
		extends AbstractLoopHandlerBuilder[B](structureBuilder) {
	def build: B = doBuild(List(whileActionBuilder withTestFunction testFunction withNextTrue chain inGroups structureBuilder.getCurrentGroups))
}