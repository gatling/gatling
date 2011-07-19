package com.excilys.ebi.gatling.examples.http

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.feeder.TSVFeeder
import com.excilys.ebi.gatling.core.context.FromContext

import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder.{ scenario, chain, HttpScenarioBuilder }
import com.excilys.ebi.gatling.http.runner.builder.HttpRunnerBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpRegExpCaptureBuilder.regexp
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpXPathCaptureBuilder.xpath
import com.excilys.ebi.gatling.http.request.builder.GetHttpRequestBuilder.get
import com.excilys.ebi.gatling.http.request.builder.PostHttpRequestBuilder.post

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

object HttpExample {
  def run = {
    val iterations = 10
    val concurrentUsers = 5
    val pause1 = 3
    val pause2 = 2
    val pause3 = 1

    val url = "http://localhost/index.html"

    val usersInformation = new TSVFeeder("test_feeder", List("login", "password"))

    val lambdaUser =
      scenario("Standard User")
        .doHttpRequest("Page d'accueil", get(url))
        .pause(pause1)
        .iterate(
          iterations,
          chain.doHttpRequest("Catégorie Poney",
            get(url),
            xpath("//input[@value='aaaa']/@id") in "ctxParam" build)
            .pause(pause2)
            .doHttpRequest("Create Thing blabla", post("http://localhost:3000/things") withQueryParam ("postTest", FromContext("ctxParam")) withTemplateBody ("create_thing", Map("name" -> "blabla")) asJSON)
            .pause(pause3)
            .doHttpRequest("Liste Articles", get("http://localhost:3000/things") withQueryParam ("test", usersInformation.get("password")_))
            .pause(pause3)
            .doHttpRequest("Create Thing omgomg", post("http://localhost:3000/things") withQueryParam ("postTest", "homeURL") withTemplateBody ("create_thing", Map("name" -> "omgomg")) asJSON))
          .doHttpRequest("Ajout au panier",
            get(url),
            regexp("""<input id="text1" type="text" value="(.*)" />""") in "input" build)
            .pause(pause3)

    prepareSimulationFor(lambdaUser) withUsersNumber concurrentUsers withFeeder usersInformation play
  }
}