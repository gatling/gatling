package advanced
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._

class AdvancedExampleSimulation extends Simulation {

	def apply = {

		val urlBase = "http://excilys-bank-web.cloudfoundry.com"

		val httpConf = httpConfig.baseURL(urlBase).disableFollowRedirect

		List(
			SomeScenario.scn.configure.users(10).ramp(10).protocolConfig(httpConf),
			SomeOtherScenario.otherScn.configure.users(5).ramp(20).delay(30).protocolConfig(httpConf))
	}
}
