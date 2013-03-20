package advanced

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import scala.concurrent.duration._

class AdvancedExampleSimulation extends Simulation {

	val httpConf = httpConfig.baseURL("http://excilys-bank-web.cloudfoundry.com").disableFollowRedirect

	setUp(
		SomeScenario.scn.inject(ramp(10 users) over(10 seconds)).protocolConfig(httpConf),
		SomeOtherScenario.otherScn.inject(delay(30 seconds), ramp(5 users) over(20 seconds)).protocolConfig(httpConf))
}
