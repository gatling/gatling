package com.excilys.ebi.gatling.http.scenario

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder._
import com.excilys.ebi.gatling.core.action.Action

import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
import com.excilys.ebi.gatling.http.request.HttpRequest

import com.ning.http.client.Request

object HttpScenarioBuilder {
  class HttpScenarioBuilder(var actionBuilders: List[AbstractActionBuilder]) {
    def actionsList = actionBuilders

    def pause(delayInMillis: Long): HttpScenarioBuilder = {
      val pause = pauseActionBuilder withDelay delayInMillis
      println("Adding PauseAction")
      new HttpScenarioBuilder(pause :: actionBuilders)
    }

    def iterate(times: Integer, chain: HttpScenarioBuilder): HttpScenarioBuilder = {
      val chainActions: List[AbstractActionBuilder] = chain.actionsList
      var iteratedActions = List[AbstractActionBuilder]()
      for (i <- 1 to times) {
        iteratedActions = chainActions ::: iteratedActions
      }
      println("Adding Iterations")
      new HttpScenarioBuilder(iteratedActions ::: actionBuilders)
    }

    def end = {
      println("Adding EndAction")
      new HttpScenarioBuilder(endActionBuilder :: actionBuilders)
    }

    def build(): Action = {
      var previousInList: Action = null
      for (actionBuilder <- actionBuilders) {
        println("previousInList: " + previousInList)
        previousInList = actionBuilder withNext (previousInList) build
      }
      println(previousInList)
      previousInList
    }

    def withNext(next: Action) = null

    def doHttpRequest(request: Request): HttpScenarioBuilder = {
      val httpRequest = httpRequestActionBuilder withRequest (new HttpRequest(request))
      println("Adding HttpRequestAction")
      new HttpScenarioBuilder(httpRequest :: actionBuilders)
    }
  }
  def scenario = new HttpScenarioBuilder(Nil)
  def chain = scenario
}