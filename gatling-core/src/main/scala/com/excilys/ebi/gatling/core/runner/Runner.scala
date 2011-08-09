package com.excilys.ebi.gatling.core.runner

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.scenario.configuration.builder.ScenarioConfigurationBuilder._

import java.util.concurrent.TimeUnit

abstract class Runner(val scenarioConfigurationBuilders: List[ScenarioConfigurationBuilder]) extends Logging {
  def run(): String
}