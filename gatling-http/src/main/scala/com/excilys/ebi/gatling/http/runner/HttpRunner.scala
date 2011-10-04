package com.excilys.ebi.gatling.http.runner

import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.scenario.configuration.builder.ScenarioConfigurationBuilder
import com.excilys.ebi.gatling.http.action.HttpRequestAction
import org.joda.time.DateTime

object HttpRunner {
  class HttpRunner(startDate: DateTime, configurationBuilders: List[ScenarioConfigurationBuilder])
    extends Runner(startDate, configurationBuilders, () => HttpRequestAction.CLIENT.close)

  def runSim(startDate: DateTime)(scenarioConfigurations: ScenarioConfigurationBuilder*) = new HttpRunner(startDate, scenarioConfigurations.toList).run
}