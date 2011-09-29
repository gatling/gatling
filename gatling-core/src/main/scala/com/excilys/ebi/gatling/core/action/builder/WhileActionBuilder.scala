package com.excilys.ebi.gatling.core.action.builder
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.context.Context
import akka.actor.TypedActor
import com.excilys.ebi.gatling.core.action.WhileAction
import akka.actor.Actor

object WhileActionBuilder {
  class WhileActionBuilder(val testFunction: Option[Context => Boolean], val nextTrue: Option[AbstractActionBuilder], val next: Option[Action])
      extends AbstractActionBuilder {

    def withTestFunction(testFunction: Context => Boolean) = new WhileActionBuilder(Some(testFunction), nextTrue, next)

    def withNextTrue(nextTrue: AbstractActionBuilder) = new WhileActionBuilder(testFunction, Some(nextTrue), next)

    def withNext(next: Action) = new WhileActionBuilder(testFunction, nextTrue, Some(next))

    def build(scenarioId: Int): Action = {
      logger.debug("Building IfAction")

      TypedActor.newInstance(classOf[Action], new WhileAction(testFunction.get, (w: WhileAction) => nextTrue.get.withNext(w).build(scenarioId), next.get))

    }
  }

  def whileActionBuilder = new WhileActionBuilder(None, None, None)
}