package com.excilys.ebi.gatling.http.runner

import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.scenario.configuration.builder.ScenarioConfigurationBuilder._

import com.excilys.ebi.gatling.http.action.HttpRequestAction

import java.util.Date

object HttpRunner {
  class HttpRunner(startDate: Date, configurationBuilders: List[ScenarioConfigurationBuilder])
    extends Runner(startDate, configurationBuilders, () => HttpRequestAction.CLIENT.close)

  def runSim(startDate: Date)(scenarioConfigurations: ScenarioConfigurationBuilder*) = new HttpRunner(startDate, scenarioConfigurations.toList).run
}