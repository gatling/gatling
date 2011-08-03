import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.feeder.TSVFeeder
import com.excilys.ebi.gatling.core.context.FromContext

import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder._
import com.excilys.ebi.gatling.http.runner.builder.HttpRunnerBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpRegExpCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpXPathCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.assertion.builder.HttpXPathAssertionBuilder._
import com.excilys.ebi.gatling.http.processor.assertion.builder.HttpRegExpAssertionBuilder._
import com.excilys.ebi.gatling.http.processor.assertion.builder.HttpStatusAssertionBuilder._
import com.excilys.ebi.gatling.http.request.builder.GetHttpRequestBuilder._
import com.excilys.ebi.gatling.http.request.builder.PostHttpRequestBuilder._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.Request

import java.util.concurrent.TimeUnit

val concurrentUsers = 1

val url = "http://mail.bluepyth.fr"
val pause1 = 3

val lambdaUser = scenario("Standard User").doHttpRequest("Redirect TEST", get(url) followsRedirect true, assertRegexp("""<div class="boxtitle">(.*)</div>""", "Bienvenue sur BluePyth Webmail") build).pause(pause1)
      
val execution = prepareSimulationFor(lambdaUser) withUsersNumber concurrentUsers withRamp (10000, TimeUnit.MILLISECONDS) play
