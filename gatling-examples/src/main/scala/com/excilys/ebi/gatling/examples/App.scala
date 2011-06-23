package com.excilys.ebi.gatling.examples

import com.excilys.ebi.gatling.core.action.Action

import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder._
import com.excilys.ebi.gatling.http.context.builder.HttpContextBuilder._
import com.excilys.ebi.gatling.http.context.HttpContext
import com.excilys.ebi.gatling.http.runner.HttpRunner._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object App {
  def main(args: Array[String]) {
    val iterations = 5
    val concurrentUsers = 2
    val pause1 = 3000L
    val pause2 = 2000L
    val pause3 = 1000L

    val url = "http://localhost/index.xhtml"
    val request: Request = new RequestBuilder setUrl url build

    val s: Action =
      scenario.doHttpRequest(request)
        .pause(pause1)
        .iterate(
          iterations,
          chain.doHttpRequest(request)
            .pause(pause2))
          .doHttpRequest(request)
          .pause(pause3)
          .end
          .build

    play(s, concurrentUsers)
  }
}
