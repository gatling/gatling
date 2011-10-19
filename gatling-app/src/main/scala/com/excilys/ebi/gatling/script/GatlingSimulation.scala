package com.excilys.ebi.gatling.script

import com.excilys.ebi.gatling.core.runner.Runner._
import org.joda.time.DateTime

trait GatlingSimulation extends App {
	def runSimulations = runSim(new DateTime(args(0)))_
}