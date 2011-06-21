package com.excilys.ebi.gatling.http.action.builder

import com.excilys.ebi.gatling.http.request.HttpRequest

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.RequestAction
import com.excilys.ebi.gatling.core.action.AbstractAction
import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.assertion.AbstractAssertion
import com.excilys.ebi.gatling.core.capture.provider.AbstractCaptureProvider

import com.excilys.ebi.gatling.http.action.HttpRequestAction
import com.excilys.ebi.gatling.http.processor.HttpProcessor

import akka.actor.TypedActor

object HttpRequestActionBuilder {
  class HttpRequestActionBuilder(val request: Option[AbstractRequest], val nextActionBuilder: Option[AbstractActionBuilder], val processors: Option[List[HttpProcessor]])
    extends AbstractActionBuilder {

    def withRequest(request: HttpRequest) = new HttpRequestActionBuilder(Some(request), nextActionBuilder, processors)

    def withNext(next: AbstractActionBuilder) = new HttpRequestActionBuilder(request, Some(next), processors)

    def build(): AbstractAction =
      TypedActor.newInstance(classOf[AbstractAction], () => new HttpRequestAction(nextActionBuilder.get.build, request.get, None))
  }

  def httpRequestActionBuilder = new HttpRequestActionBuilder(None, None, None)
}

