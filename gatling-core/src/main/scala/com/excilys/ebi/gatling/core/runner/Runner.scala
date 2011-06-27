package com.excilys.ebi.gatling.core.runner

import com.excilys.ebi.gatling.core.scenario.ScenarioBuilder
import com.excilys.ebi.gatling.core.log.Logging

abstract class Runner(val scenarioBuilder: ScenarioBuilder, val numberOfUsers: Integer, val rampTime: Option[Integer]) extends Logging {
  def run()
}