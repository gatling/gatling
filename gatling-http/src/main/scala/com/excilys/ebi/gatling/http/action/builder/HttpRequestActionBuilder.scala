package com.excilys.ebi.gatling.http.action.builder

import com.excilys.ebi.gatling.http.request.HttpRequest

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder

import com.excilys.ebi.gatling.http.action.HttpRequestAction
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder

import akka.actor.TypedActor

object HttpRequestActionBuilder {
  class HttpRequestActionBuilder(val request: Option[HttpRequest], val nextAction: Option[Action], val processorBuilders: Option[List[HttpProcessorBuilder]], val groups: Option[List[String]])
      extends AbstractActionBuilder {

    def withProcessors(givenProcessors: List[HttpProcessorBuilder]) = {
      logger.debug("Adding Processors")
      new HttpRequestActionBuilder(request, nextAction, Some(givenProcessors ::: processorBuilders.getOrElse(Nil)), groups)
    }

    def withProcessor(processor: HttpProcessorBuilder) = new HttpRequestActionBuilder(request, nextAction, Some(processor :: processorBuilders.getOrElse(Nil)), groups)

    def withRequest(request: HttpRequest) = new HttpRequestActionBuilder(Some(request), nextAction, processorBuilders, groups)

    def withNext(next: Action) = new HttpRequestActionBuilder(request, Some(next), processorBuilders, groups)

    def inGroups(groups: List[String]) = new HttpRequestActionBuilder(request, nextAction, processorBuilders, Some(groups))

    def build(scenarioId: Int): Action = {
      logger.debug("Building HttpRequestAction with request {}", request.get)
      TypedActor.newInstance(classOf[Action], new HttpRequestAction(nextAction.get, request.get, processorBuilders, groups.get))
    }

  }

  def newHttpRequestActionBuilder = new HttpRequestActionBuilder(None, None, None, Some(Nil))
}

