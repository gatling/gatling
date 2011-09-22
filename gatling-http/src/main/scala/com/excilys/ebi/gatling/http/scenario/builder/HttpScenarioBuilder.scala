package com.excilys.ebi.gatling.http.scenario.builder

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._

import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder

object HttpScenarioBuilder {
  class HttpScenarioBuilder(name: String, actionBuilders: List[AbstractActionBuilder]) extends ScenarioBuilder[HttpScenarioBuilder](name, actionBuilders) {

    def doHttpRequest(reqName: String, requestBuilder: AbstractHttpRequestBuilder[_], processors: HttpProcessorBuilder*): HttpScenarioBuilder = {
      val httpRequest = newHttpRequestActionBuilder withRequest (new HttpRequest(reqName, requestBuilder)) withProcessors processors.toList
      logger.debug("Adding HttpRequestAction")
      new HttpScenarioBuilder(name, httpRequest :: actionBuilders)
    }

    def newInstance(name: String, actionBuilders: List[AbstractActionBuilder]): HttpScenarioBuilder = {
      new HttpScenarioBuilder(name, actionBuilders)
    }
  }
  def scenario(name: String) = new HttpScenarioBuilder(name, Nil)
  def chain = scenario("")
}