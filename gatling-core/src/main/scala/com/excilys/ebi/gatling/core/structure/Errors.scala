package com.excilys.ebi.gatling.core.structure

import java.util.UUID

import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder
import com.excilys.ebi.gatling.core.action.builder.TryMaxActionBuilder.tryMaxActionBuilder
import com.excilys.ebi.gatling.core.session.Session

trait Errors[B] extends Execs[B] {

	def exitBlockOnFail(chain: ChainBuilder): B = tryMax(1)(chain)
	def tryMax(times: Int)(chain: ChainBuilder): B = tryMax(times, None)(chain)
	def tryMax(times: Int, counterName: String)(chain: ChainBuilder): B = tryMax(times, Some(counterName))(chain)
	private def tryMax(times: Int, counterName: Option[String])(chain: ChainBuilder): B = {

		def buildTransactionalChain(chain: ChainBuilder): ChainBuilder = {
			val startBlock = SimpleActionBuilder((session: Session) => session.clearFailed.setMustExitOnFail)
			val endBlock = SimpleActionBuilder((session: Session) => session.clearMustExitOnFail)
			ChainBuilder.emptyChain.exec(startBlock).exec(chain).exec(endBlock)
		}

		times match {
			case times if times >= 1 =>
				val loopCounterName = counterName.getOrElse(UUID.randomUUID.toString)
				exec(tryMaxActionBuilder.withTimes(times).withLoopNext(buildTransactionalChain(chain)).withCounterName(loopCounterName))

			case times => throw new IllegalArgumentException("Can't set up a max try <= 1")
		}
	}

	def exitHereIfFailed: B = exec(SimpleActionBuilder((session: Session) => session.setMustExitOnFail))
}