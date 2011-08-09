package com.excilys.ebi.gatling.http.action.builder

import com.excilys.ebi.gatling.http.request.HttpRequest

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.RequestAction
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder

import com.excilys.ebi.gatling.http.action.HttpRequestAction
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.http.scenario.builder.HttpScenarioBuilder

import akka.actor.TypedActor

object HttpRequestActionBuilder {
  class HttpRequestActionBuilder(val request: Option[HttpRequest], val nextAction: Option[Action], val processorBuilders: Option[List[HttpProcessorBuilder]])
      extends AbstractActionBuilder {

    def withProcessors(givenProcessors: List[HttpProcessorBuilder]) = {
      logger.debug("Adding Processors")
      new HttpRequestActionBuilder(request, nextAction, Some(givenProcessors ::: processorBuilders.getOrElse(Nil)))
    }

    def withProcessor(processor: HttpProcessorBuilder) = new HttpRequestActionBuilder(request, nextAction, Some(processor :: processorBuilders.getOrElse(Nil)))

    def withRequest(request: HttpRequest) = new HttpRequestActionBuilder(Some(request), nextAction, processorBuilders)

    def withNext(next: Action) = new HttpRequestActionBuilder(request, Some(next), processorBuilders)

    def build(scenarioId: Int): Action = {
      logger.debug("Building HttpRequestAction with request {}", request.get)
      ScenarioBuilder.addRelevantAction(scenarioId)
      TypedActor.newInstance(classOf[Action], new HttpRequestAction(nextAction.get, request.get, processorBuilders))
    }

  }

  def httpRequestActionBuilder = new HttpRequestActionBuilder(None, None, None)
}

