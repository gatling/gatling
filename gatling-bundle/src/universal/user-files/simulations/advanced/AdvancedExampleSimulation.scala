package advanced

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.Headers.Names._
import scala.concurrent.duration._

class AdvancedExampleSimulation extends Simulation {

	val httpProtocol = http.baseURL("http://excilysbank.gatling.cloudbees.net").disableFollowRedirect

	setUp(SomeScenario.scn.inject(ramp(10 users) over (10 seconds)),
		SomeOtherScenario.otherScn.inject(nothingFor(30 seconds), ramp(5 users) over (20 seconds)))
		.protocols(httpProtocol)
}
