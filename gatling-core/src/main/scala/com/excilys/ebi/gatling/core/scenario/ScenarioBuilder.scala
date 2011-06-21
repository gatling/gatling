package com.excilys.ebi.gatling.core.scenario

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder.EndActionBuilder
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._

object ScenarioBuilder {
  class ScenarioBuilder(val actions: Option[List[AbstractActionBuilder]]) extends AbstractActionBuilder {

    def actionsList = actions

    def then(action: AbstractActionBuilder): ScenarioBuilder = {
      actions.get.first.withNext(action)
      new ScenarioBuilder(Some(action :: actions.get))
    }

    def pause(delayInMillis: Long): ScenarioBuilder = {
      val pause = pauseActionBuilder withDelay delayInMillis
      actions.get.first.withNext(pause)
      new ScenarioBuilder(Some(pause :: actions.get))
    }

    def iterate(times: Integer, chain: ScenarioBuilder): ScenarioBuilder = {
      val chainActions: List[AbstractActionBuilder] = chain.actionsList.get
      var iteratedActions: List[AbstractActionBuilder] = chainActions
      for (i <- 1 until times) {
        iteratedActions.first.withNext(chainActions.last)
        chainActions :: iteratedActions
      }
      actions.get.first.withNext(iteratedActions.last)
      new ScenarioBuilder(Some(iteratedActions ::: actions.get))
    }

    def end: AbstractActionBuilder = new EndActionBuilder

    def build(): Action = actions.get.first.build

    def withNext(next: AbstractActionBuilder) = actions.get.first.withNext(next)

  }

  def scenario(firstAction: AbstractActionBuilder) = new ScenarioBuilder(Some(firstAction :: Nil))
  def chain(firstAction: AbstractActionBuilder) = scenario(firstAction)
}