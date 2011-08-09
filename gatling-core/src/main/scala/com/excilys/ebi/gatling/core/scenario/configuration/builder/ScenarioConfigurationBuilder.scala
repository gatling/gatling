package com.excilys.ebi.gatling.core.scenario.configuration.builder

import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfiguration

import java.util.concurrent.TimeUnit

object ScenarioConfigurationBuilder {
  class ScenarioConfigurationBuilder(s: Option[ScenarioBuilder], numUsers: Option[Int], ramp: Option[(Int, TimeUnit)], startTime: Option[(Int, TimeUnit)]) {

    def withUsersNumber(nbUsers: Int) = new ScenarioConfigurationBuilder(s, Some(nbUsers), ramp, startTime)

    def withRampOf(rampTime: Int): ScenarioConfigurationBuilder = withRampOf(rampTime, TimeUnit.SECONDS)

    def withRampOf(rampTime: Int, unit: TimeUnit) = new ScenarioConfigurationBuilder(s, numUsers, Some((rampTime, unit)), startTime)

    def startsAt(startTime: Int): ScenarioConfigurationBuilder = startsAt(startTime, TimeUnit.SECONDS)

    def startsAt(startTime: Int, unit: TimeUnit) = new ScenarioConfigurationBuilder(s, numUsers, ramp, Some((startTime, unit)))

    def build(scenarioId: Int): ScenarioConfiguration = new ScenarioConfiguration(scenarioId, s.get, numUsers.get, ramp.get, startTime.get)
  }
  def configureScenario(s: ScenarioBuilder) = new ScenarioConfigurationBuilder(Some(s), Some(500), Some((0, TimeUnit.SECONDS)), Some((0, TimeUnit.SECONDS)))
}