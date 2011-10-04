package com.excilys.ebi.gatling.http.scenario.builder

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder
import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.core.action.Action
import org.apache.commons.lang3.StringUtils

object HttpScenarioBuilder {
  class HttpScenarioBuilder(name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action], groups: Option[List[String]])
      extends ScenarioBuilder[HttpScenarioBuilder](name, actionBuilders, next, groups) {

    def doHttpRequest(reqName: String, requestBuilder: AbstractHttpRequestBuilder[_], processors: HttpProcessorBuilder*): HttpScenarioBuilder = {
      val httpRequest = newHttpRequestActionBuilder withRequest (new HttpRequest(reqName, requestBuilder)) withProcessors processors.toList
      logger.debug("Adding HttpRequestAction")
      new HttpScenarioBuilder(name, httpRequest :: actionBuilders, next, groups)
    }

    def newInstance(name: String, actionBuilders: List[AbstractActionBuilder], next: Option[Action], groups: Option[List[String]]): HttpScenarioBuilder = {
      new HttpScenarioBuilder(name, actionBuilders, next, groups)
    }
  }
  def scenario(name: String) = new HttpScenarioBuilder(name, Nil, None, Some(Nil))
  def chain = scenario(StringUtils.EMPTY)
}