package com.excilys.ebi.gatling.http.scenario

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder._
import com.excilys.ebi.gatling.core.action.Action

import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.processor.HttpProcessor

import com.ning.http.client.Request

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HttpScenarioBuilder {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[HttpScenarioBuilder]);
  var numberOfRelevantActions = 0

  def addRelevantAction = { numberOfRelevantActions += 1 }

  class HttpScenarioBuilder(var actionBuilders: List[AbstractActionBuilder]) {

    def actionsList = actionBuilders
    def getNumberOfRelevantActions = numberOfRelevantActions

    def pause(delayInMillis: Long): HttpScenarioBuilder = {
      val pause = pauseActionBuilder withDelay delayInMillis
      LOGGER.debug("Adding PauseAction")
      new HttpScenarioBuilder(pause :: actionBuilders)
    }

    def iterate(times: Integer, chain: HttpScenarioBuilder): HttpScenarioBuilder = {
      val chainActions: List[AbstractActionBuilder] = chain.actionsList
      var iteratedActions = List[AbstractActionBuilder]()
      for (i <- 1 to times) {
        iteratedActions = chainActions ::: iteratedActions
      }
      LOGGER.debug("Adding Iterations")
      new HttpScenarioBuilder(iteratedActions ::: actionBuilders)
    }

    def end = {
      LOGGER.debug("Adding EndAction")
      new HttpScenarioBuilder(endActionBuilder :: actionBuilders)
    }

    def build(): Action = {
      var previousInList: Action = null
      for (actionBuilder <- actionBuilders) {
        LOGGER.debug("previousInList: {}", previousInList)
        previousInList = actionBuilder withNext (previousInList) build
      }
      println(previousInList)
      previousInList
    }

    def withNext(next: Action) = null

    def doHttpRequest(request: Request, processors: List[HttpProcessor]): HttpScenarioBuilder = {
      val httpRequest = httpRequestActionBuilder withRequest (new HttpRequest(request)) withProcessors processors
      LOGGER.debug("Adding HttpRequestAction")
      new HttpScenarioBuilder(httpRequest :: actionBuilders)
    }

    def doHttpRequest(request: Request): HttpScenarioBuilder = {
      doHttpRequest(request, Nil)
    }
  }
  def scenario = new HttpScenarioBuilder(Nil)
  def chain = scenario
}