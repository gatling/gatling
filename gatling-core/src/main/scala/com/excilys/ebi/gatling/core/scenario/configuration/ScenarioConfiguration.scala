package com.excilys.ebi.gatling.core.scenario.configuration

import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._

import java.util.concurrent.TimeUnit

class ScenarioConfiguration(val scenarioId: Int, val scenarioBuilder: ScenarioBuilder[_ <: ScenarioBuilder[_]], val numberOfUsers: Int, val ramp: (Int, TimeUnit),
  val startTime: (Int, TimeUnit), val feeder: Option[Feeder]) {
  def numberOfRelevantActions = (ScenarioBuilder.getNumberOfRelevantActionsByScenario.get(scenarioId).get + 1) * numberOfUsers
}