package com.excilys.ebi.gatling.core.structure.loop.handler.builder
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder._
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.structure.builder.AbstractStructureBuilder
import com.excilys.ebi.gatling.core.structure.builder.ChainBuilder

class TimesLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, times: Int)
		extends AbstractLoopHandlerBuilder[B](structureBuilder) {

	def build: B = {

		val chainActions: List[AbstractActionBuilder] = chain.getActionBuilders ::: List(simpleActionBuilder((c: Context) => c.incrementCounter))
		var iteratedActions: List[AbstractActionBuilder] = Nil
		for (i <- 1 to times) {
			iteratedActions = chainActions ::: iteratedActions
		}
		iteratedActions = simpleActionBuilder((c: Context) => c.expireCounter) :: iteratedActions
		logger.debug("Adding {} Iterations", times)

		doBuild(iteratedActions)
	}
}