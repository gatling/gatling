import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.script.GatlingSimulation

class Simulation extends GatlingSimulation {

	val urlBase = "http://excilys-bank-web.cloudfoundry.com"

	val httpConf = httpConfig.baseURL(urlBase)

	runSimulation(
		SomeScenario.scn.configure users 10 ramp 10 protocolConfig httpConf,
		SomeOtherScenario.otherScn.configure users 5 ramp 20 delay 30 protocolConfig httpConf)
}
