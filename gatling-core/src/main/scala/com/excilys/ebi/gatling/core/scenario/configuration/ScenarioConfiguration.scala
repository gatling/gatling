package com.excilys.ebi.gatling.core.scenario.configuration

import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._

import java.util.concurrent.TimeUnit

class ScenarioConfiguration(val scenarioId: Int, val scenarioBuilder: ScenarioBuilder, val numberOfUsers: Int, val ramp: (Int, TimeUnit), val startTime: (Int, TimeUnit)) {
  def numberOfRelevantActions = (ScenarioBuilder.getNumberOfRelevantActionsByScenario.get(scenarioId).get + 1) * numberOfUsers
}