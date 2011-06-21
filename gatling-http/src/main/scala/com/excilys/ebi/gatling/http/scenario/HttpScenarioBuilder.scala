package com.excilys.ebi.gatling.http.scenario

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.scenario.ScenarioBuilder.ScenarioBuilder
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder._
import com.excilys.ebi.gatling.core.action.AbstractAction

import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
import com.excilys.ebi.gatling.http.request.HttpRequest

import com.ning.http.client.Request

object HttpScenarioBuilder {
  class HttpScenarioBuilder(var actionBuilders: List[AbstractActionBuilder]) {
    def actionsList = actionBuilders

    def pause(delayInMillis: Long): HttpScenarioBuilder = {
      val pause = pauseActionBuilder withDelay delayInMillis
      val newActionBuilders =
        if (actionBuilders.size > 0) {
          actionBuilders.first.withNext(pause) :: actionBuilders.tail
        } else {
          actionBuilders
        }
      new HttpScenarioBuilder(pause :: newActionBuilders)
    }

    def iterate(times: Integer, chain: HttpScenarioBuilder): HttpScenarioBuilder = {
      val chainActions: List[AbstractActionBuilder] = chain.actionsList
      var iteratedActions = chainActions
      for (i <- 1 until times) {
        iteratedActions = iteratedActions.first.withNext(chainActions.last) :: iteratedActions.tail
        iteratedActions = chainActions ::: iteratedActions
      }
      val newActionBuilders =
        if (actionBuilders.size > 0) {
          actionBuilders.first.withNext(iteratedActions.last) :: actionBuilders.tail
        } else {
          actionBuilders
        }
      new HttpScenarioBuilder(iteratedActions ::: newActionBuilders)
    }

    def end = {
      val endBuilder = endActionBuilder
      val newActionBuilders =
        if (actionBuilders.size > 0) {
          actionBuilders.first.withNext(endBuilder) :: actionBuilders.tail
        } else {
          actionBuilders
        }
      new HttpScenarioBuilder(endBuilder :: newActionBuilders)
    }

    def build(): AbstractAction = {
      actionBuilders.last.build
    }

    def withNext(next: AbstractActionBuilder) = actionBuilders.first.withNext(next)

    def doHttpRequest(request: Request): HttpScenarioBuilder = {
      val httpRequest = httpRequestActionBuilder withRequest (new HttpRequest(request))
      val newActionBuilders =
        if (actionBuilders.size > 0) {
          actionBuilders.first.withNext(httpRequest) :: actionBuilders.tail
        } else {
          actionBuilders
        }
      new HttpScenarioBuilder(httpRequest :: newActionBuilders)
    }
  }
  def scenario = new HttpScenarioBuilder(Nil)
  def chain = scenario
}