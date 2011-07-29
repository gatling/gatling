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

import java.util.concurrent.TimeUnit

val iterations = 5
val concurrentUsers = 5
val pause1 = 3
val pause2 = 2
val pause3 = 1

val url = "http://localhost:3000"

val usersCredentials = new TSVFeeder("user_credential", List("login", "password"))
val usersInformation = new TSVFeeder("user_information", List("firstname", "lastname"))

val lambdaUser =
  scenario("Standard User")
    // First request outside iteration
    .doHttpRequest(
      "Catégorie Poney",
      get(url))
    .pause(pause1)
    // Loop
    .iterate(
      // How many times ?
      iterations,
      // What will be repeated ?
      chain
        // First request to be repeated
        .doHttpRequest(
          "Page accueil",
          get(url),
          xpath("//input[@value='aaaa']/@id") in "ctxParam" build)
        .pause(pause2)
        // Second request to be repeated
        .doHttpRequest(
          "Create Thing blabla",
          post("http://localhost:3000/things") followsRedirect true withFeeder usersCredentials withQueryParam "login" withQueryParam "password" withTemplateBody ("create_thing", Map("name" -> "blabla")) asJSON)
        .pause(pause3)
        // Third request to be repeated
        .doHttpRequest(
          "Liste Articles",
          get("http://localhost:3000/things") withFeeder usersInformation withQueryParam "firstname" withQueryParam "lastname",
          regexp("""Cookie:(\w+)""") in "testCookie" build)
        .pause(pause3)
        // Fourth request to be repeated
        .doHttpRequest(
          "Create Thing omgomg",
          post("http://localhost:3000/things") withQueryParam ("postTest", FromContext("ctxParam")) withTemplateBodyFromContext ("create_thing", Map("name" -> "ctxParam")) asJSON))
    // Second request outside iteration
    .doHttpRequest("Ajout au panier",
      get(url),
      regexp("""<input id="text1" type="text" value="(.*)" />""") in "input" build)
    .pause(pause3)

val execution = 
  prepareSimulationFor(lambdaUser) withUsersNumber concurrentUsers withRamp (10000, TimeUnit.MILLISECONDS) play
