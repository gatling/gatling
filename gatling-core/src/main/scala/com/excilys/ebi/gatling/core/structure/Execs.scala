package com.excilys.ebi.gatling.core.structure

import com.excilys.ebi.gatling.core.action.builder.{ ActionBuilder, BypassSimpleActionBuilder }
import com.excilys.ebi.gatling.core.session.Session

trait Execs[B] {

	private[core] def actionBuilders: List[ActionBuilder]
	private[core] def newInstance(actionBuilders: List[ActionBuilder]): B
	private[core] def getInstance: B

	/**
	 * Method used to execute an action
	 *
	 * @param actionBuilder the action builder representing the action to be executed
	 */
	def exec(sessionFunction: Session => Session): B = exec(BypassSimpleActionBuilder(sessionFunction))
	def exec(actionBuilder: ActionBuilder): B = newInstance(actionBuilder :: actionBuilders)
	def exec(chains: ChainBuilder*): B = exec(chains.toIterable)
	def exec(chains: Iterator[ChainBuilder]): B = exec(chains.toIterable)
	def exec(chains: Iterable[ChainBuilder]): B = newInstance(chains.toList.reverse.map(_.actionBuilders).flatten ::: actionBuilders)

	/**
	 * Method used to insert an existing chain inside the current scenario
	 *
	 * @param chain the chain to be included in the scenario
	 * @return a new builder with all actions from the chain added to its actions
	 */
	@deprecated("""Will be removed in Gatling 1.4.0. Use "exec" instead.""", "1.3.0")
	def insertChain(chain: ChainBuilder): B = newInstance(chain.actionBuilders ::: actionBuilders)
}