package com.excilys.ebi.gatling.examples

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder

import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder.{ scenario, chain, HttpScenarioBuilder }
import com.excilys.ebi.gatling.http.runner.HttpRunner.play
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpRegExpCaptureBuilder.regexp
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpXPathCaptureBuilder.xpath

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object App {
  def main(args: Array[String]) {
    val iterations = 10
    val concurrentUsers = 5
    val pause1 = 3000L
    val pause2 = 2000L
    val pause3 = 1000L

    val url = "http://localhost/index.html"
    val request: Request = new RequestBuilder setUrl url build

    val lambdaUser =
      scenario("Standard User")
        .doHttpRequest("Page d'accueil", request)
        .pause(pause1)
        .iterate(
          iterations,
          chain.doHttpRequest("Catégorie Poney",
            request,
            xpath("//input[@value='aaaa']/@id") inAttribute "inputbis" build)
            .pause(pause2)
            .doHttpRequest("Liste Articles", request)
            .pause(pause3))
          .doHttpRequest(
            "Ajout au panier",
            request,
            regexp("""<input id="text1" type="text" value="(.*)" />""") inAttribute "input" build)
            .pause(pause3)

    play(lambdaUser, concurrentUsers)
  }
}
