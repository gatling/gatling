package com.excilys.ebi.gatling.core.structure.loop.handler.builder
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder._
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.structure.builder.AbstractStructureBuilder
import com.excilys.ebi.gatling.core.structure.builder.ChainBuilder
import akka.actor.TypedActor
import com.excilys.ebi.gatling.core.action.Action
import akka.actor.Uuid

class TimesLoopHandlerBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, times: Int, counterName: Option[String])
		extends AbstractLoopHandlerBuilder[B](structureBuilder) {

	def build: B = {

		val counter = counterName.getOrElse(new Uuid().toString)

		val chainActions: List[AbstractActionBuilder] = chain.getActionBuilders ::: List(simpleActionBuilder((c: Context, a: Action) => c.incrementCounter(counter)))
		var iteratedActions: List[AbstractActionBuilder] = Nil

		for (i <- 1 to times)
			iteratedActions = chainActions ::: iteratedActions

		iteratedActions = simpleActionBuilder((c: Context, a: Action) => c.removeCounter(counter)) :: iteratedActions ::: List(simpleActionBuilder((c: Context, a: Action) => c.startCounter(counter)))

		logger.debug("Adding {} Iterations", times)

		doBuild(iteratedActions)
	}
}