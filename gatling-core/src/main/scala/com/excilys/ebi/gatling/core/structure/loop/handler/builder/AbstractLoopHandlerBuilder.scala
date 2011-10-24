package com.excilys.ebi.gatling.core.structure.loop.handler.builder
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.structure.builder.AbstractStructureBuilder

abstract class AbstractLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B) extends Logging {
	def build: B

	protected def doBuild(actionBuilders: List[AbstractActionBuilder]) = structureBuilder.addActionBuilders(actionBuilders)
}