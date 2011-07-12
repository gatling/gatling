package com.excilys.ebi.gatling.http.action.builder

import com.excilys.ebi.gatling.http.request.HttpRequest

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.RequestAction
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider

import com.excilys.ebi.gatling.http.action.HttpRequestAction
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder

import akka.actor.TypedActor

object HttpRequestActionBuilder {
  class HttpRequestActionBuilder(val request: Option[HttpRequest], val nextAction: Option[Action], val processors: Option[List[HttpProcessor]])
    extends AbstractActionBuilder {

    def withProcessors(givenProcessors: List[HttpProcessor]) = {
      logger.debug("Adding Processors")
      new HttpRequestActionBuilder(request, nextAction, Some(givenProcessors ::: processors.getOrElse(Nil)))
    }

    def withProcessor(processor: HttpProcessor) = new HttpRequestActionBuilder(request, nextAction, Some(processor :: processors.getOrElse(Nil)))

    def withRequest(request: HttpRequest) = new HttpRequestActionBuilder(Some(request), nextAction, processors)

    def withNext(next: Action) = new HttpRequestActionBuilder(request, Some(next), processors)

    def build(): Action = {
      logger.debug("Building HttpRequestAction with request {} and processors: {}", request.get, processors.get)
      HttpScenarioBuilder.addRelevantAction
      TypedActor.newInstance(classOf[Action], new HttpRequestAction(nextAction.get, request.get, processors))
    }

  }

  def httpRequestActionBuilder = new HttpRequestActionBuilder(None, None, None)
}

