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
  class HttpScenarioBuilder(name: String, actionBuilders: List[AbstractActionBuilder]) extends ScenarioBuilder[HttpScenarioBuilder](name, actionBuilders) with Logging {

    def doHttpRequest(reqName: String, requestBuilder: HttpRequestBuilder, processors: HttpProcessorBuilder*): HttpScenarioBuilder = {
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