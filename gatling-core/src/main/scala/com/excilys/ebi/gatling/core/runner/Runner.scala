package com.excilys.ebi.gatling.core.runner

import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.scenario.ScenarioBuilder
import com.excilys.ebi.gatling.core.log.Logging

import java.util.concurrent.TimeUnit

abstract class Runner(val scenarioBuilder: ScenarioBuilder, val numberOfUsers: Int, val rampTime: Option[(Int, TimeUnit)]) extends Logging {
  def run(): String
}