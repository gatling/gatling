package com.excilys.ebi.gatling.core.action.builder
import com.excilys.ebi.gatling.core.action.Action
import akka.actor.TypedActor
import com.excilys.ebi.gatling.core.action.IfAction
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder

object IfActionBuilder {
  class IfActionBuilder(val testFunction: Option[Context => Boolean], val nextTrue: Option[AbstractActionBuilder], val nextFalse: Option[AbstractActionBuilder],
                        val next: Option[Action], val groups: Option[List[String]])
      extends AbstractActionBuilder {

    def withTestFunction(testFunction: Context => Boolean) = new IfActionBuilder(Some(testFunction), nextTrue, nextFalse, next, groups)

    def withNextTrue(nextTrue: AbstractActionBuilder) = new IfActionBuilder(testFunction, Some(nextTrue), nextFalse, next, groups)

    def withNextFalse(nextFalse: Option[AbstractActionBuilder]) = new IfActionBuilder(testFunction, nextTrue, nextFalse, next, groups)

    def withNext(next: Action) = new IfActionBuilder(testFunction, nextTrue, nextFalse, Some(next), groups)

    def inGroups(groups: List[String]) = new IfActionBuilder(testFunction, nextTrue, nextFalse, next, Some(groups))

    def build(scenarioId: Int): Action = {
      logger.debug("Building IfAction")

      val actionTrue = nextTrue.get.withNext(next.get).inGroups(groups.get).build(scenarioId)
      val actionFalse = nextFalse.map { chain =>
        chain.withNext(next.get).inGroups(groups.get).build(scenarioId)
      }

      TypedActor.newInstance(classOf[Action], new IfAction(testFunction.get, actionTrue, actionFalse, next.get))
    }
  }

  def ifActionBuilder = new IfActionBuilder(None, None, None, None, Some(Nil))
}