package advanced

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AdvancedExampleSimulation extends Simulation {

	val httpProtocol = http.baseURL("http://excilysbank.gatling.cloudbees.net").disableFollowRedirect

	setUp(SomeScenario.scn.inject(rampUsers(10) over (10 seconds)),
		  SomeOtherScenario.otherScn.inject(nothingFor(30 seconds), rampUsers(5) over (20 seconds)))
		.protocols(httpProtocol)
}
