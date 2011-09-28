package com.excilys.ebi.gatling.http.scenario.builder

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._
import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.core.action.Action
import org.apache.commons.lang3.StringUtils

object HttpScenarioBuilder {
  class HttpScenarioBuilder(name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action])
    extends ScenarioBuilder[HttpScenarioBuilder](name, actionBuilders, next) {

    def doHttpRequest(reqName: String, requestBuilder: AbstractHttpRequestBuilder[_], processors: HttpProcessorBuilder*): HttpScenarioBuilder = {
      val httpRequest = newHttpRequestActionBuilder withRequest (new HttpRequest(reqName, requestBuilder)) withProcessors processors.toList
      logger.debug("Adding HttpRequestAction")
      new HttpScenarioBuilder(name, httpRequest :: actionBuilders, next)
    }

    def newInstance(name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action]): HttpScenarioBuilder = {
      new HttpScenarioBuilder(name, actionBuilders, next)
    }
  }
  def scenario(name: String) = new HttpScenarioBuilder(name, Nil, None)
  def chain = scenario(StringUtils.EMPTY)
}