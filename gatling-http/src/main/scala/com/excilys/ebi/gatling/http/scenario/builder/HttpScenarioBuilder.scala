package com.excilys.ebi.gatling.http.scenario.builder

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder._
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._

import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
import com.excilys.ebi.gatling.http.request.builder.HttpRequestBuilder
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder

import com.ning.http.client.Request

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object HttpScenarioBuilder {
  class HttpScenarioBuilder(name: String, actionBuilders: List[AbstractActionBuilder]) extends ScenarioBuilder(name, actionBuilders) with Logging {

    def pause(delayValue: Int): HttpScenarioBuilder = {
      pause(delayValue, TimeUnit.SECONDS)
    }

    def pause(delayValue: Int, delayUnit: TimeUnit): HttpScenarioBuilder = {
      val pause = pauseActionBuilder withDelayValue delayValue withDelayUnit delayUnit
      logger.debug("Adding PauseAction")
      new HttpScenarioBuilder(name, pause :: actionBuilders)
    }

    def iterate(times: Int, chain: HttpScenarioBuilder): HttpScenarioBuilder = {
      val chainActions: List[AbstractActionBuilder] = chain.actionsList
      var iteratedActions: List[AbstractActionBuilder] = Nil
      for (i <- 1 to times) {
        iteratedActions = chainActions ::: iteratedActions
      }
      logger.debug("Adding {} Iterations", times)
      new HttpScenarioBuilder(name, iteratedActions ::: actionBuilders)
    }

    def end(latch: CountDownLatch) = {
      logger.debug("Adding EndAction")
      new HttpScenarioBuilder(name, endActionBuilder(latch) :: actionBuilders)
    }

    def build(scenarioId: Int): Action = {
      var previousInList: Action = null
      for (actionBuilder <- actionBuilders) {
        previousInList = actionBuilder withNext (previousInList) build (scenarioId)
      }
      println(previousInList)
      previousInList
    }

    def withNext(next: Action) = null

    def doHttpRequest(reqName: String, requestBuilder: HttpRequestBuilder, processors: HttpProcessorBuilder*): HttpScenarioBuilder = {
      val httpRequest = httpRequestActionBuilder withRequest (new HttpRequest(reqName, requestBuilder)) withProcessors processors.toList
      logger.debug("Adding HttpRequestAction")
      new HttpScenarioBuilder(name, httpRequest :: actionBuilders)
    }
  }
  def scenario(name: String) = new HttpScenarioBuilder(name, Nil)
  def chain = scenario("")
}