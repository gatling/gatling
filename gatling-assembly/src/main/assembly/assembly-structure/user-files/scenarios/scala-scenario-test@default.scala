import com.excilys.ebi.gatling.example.script.custom.Scenarios._

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._

class Simulation extends GatlingSimulation {
	/* Feeder */
	val usersInfos = new TSVFeeder("bank2", List("username", "password", "acc1", "acc2", "acc3", "acc4"))

	/* Configuration */
	val scnConf = scn.configure feeder usersInfos users 1 ramp 1

	/* Simulation */
	runSimulations(scnConf)
}