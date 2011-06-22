package com.excilys.ebi.gatling.http.action.builder

import com.excilys.ebi.gatling.http.request.HttpRequest

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.RequestAction
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.assertion.AbstractAssertion
import com.excilys.ebi.gatling.core.capture.provider.AbstractCaptureProvider

import com.excilys.ebi.gatling.http.action.HttpRequestAction
import com.excilys.ebi.gatling.http.processor.HttpProcessor

import akka.actor.TypedActor

object HttpRequestActionBuilder {
  class HttpRequestActionBuilder(val request: Option[HttpRequest], val nextAction: Option[Action], val processors: Option[List[HttpProcessor]])
    extends AbstractActionBuilder {

    def withRequest(request: HttpRequest) = new HttpRequestActionBuilder(Some(request), nextAction, processors)

    def withNext(next: Action) = new HttpRequestActionBuilder(request, Some(next), processors)

    def build(): Action = {
      println("Building HttpRequestAction")
      TypedActor.newInstance(classOf[Action], new HttpRequestAction(nextAction.get, request.get, None))
    }

    override def toString = "next: " + nextAction + ", request: " + request
  }

  def httpRequestActionBuilder = new HttpRequestActionBuilder(None, None, None)
}

