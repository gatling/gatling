import com.excilys.ebi.gatling.example.script.custom.Scenarios._

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.script.GatlingSimulation

class Simulation extends GatlingSimulation {

	/* Configuration */
	val scnConf = scn.configure users(1) ramp(1)

	val httpConf = httpConfig.baseURL("http://localhost:8080/excilys-bank-web")

	/* Simulation */
	runSimulations(scnConf.protocolConfig(httpConf))
}