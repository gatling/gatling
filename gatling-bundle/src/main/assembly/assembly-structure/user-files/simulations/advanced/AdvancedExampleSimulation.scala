package advanced

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._

class AdvancedExampleSimulation extends Simulation {

	def apply = {

		val httpConf = httpConfig.baseURL("http://excilys-bank-web.cloudfoundry.com").disableFollowRedirect

		List(
			SomeScenario.scn.configure.users(10).ramp(10).protocolConfig(httpConf),
			SomeOtherScenario.otherScn.configure.users(5).ramp(20).delay(30).protocolConfig(httpConf))
	}
}
