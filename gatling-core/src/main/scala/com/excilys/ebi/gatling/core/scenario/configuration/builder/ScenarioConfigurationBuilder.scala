package com.excilys.ebi.gatling.core.scenario.configuration.builder

import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfiguration

import java.util.concurrent.TimeUnit

object ScenarioConfigurationBuilder {
  class ScenarioConfigurationBuilder(s: Option[ScenarioBuilder], numUsers: Option[Int], ramp: Option[(Int, TimeUnit)],
                                     startTime: Option[(Int, TimeUnit)], feeder: Option[Feeder]) {

    def withUsersNumber(nbUsers: Int) = new ScenarioConfigurationBuilder(s, Some(nbUsers), ramp, startTime, feeder)

    def withRampOf(rampTime: Int): ScenarioConfigurationBuilder = withRampOf(rampTime, TimeUnit.SECONDS)

    def withRampOf(rampTime: Int, unit: TimeUnit) = new ScenarioConfigurationBuilder(s, numUsers, Some((rampTime, unit)), startTime, feeder)

    def withFeeder(feeder: Feeder) = new ScenarioConfigurationBuilder(s, numUsers, ramp, startTime, Some(feeder))

    def startsAt(startTime: Int): ScenarioConfigurationBuilder = startsAt(startTime, TimeUnit.SECONDS)

    def startsAt(startTime: Int, unit: TimeUnit) = new ScenarioConfigurationBuilder(s, numUsers, ramp, Some((startTime, unit)), feeder)

    def build(scenarioId: Int): ScenarioConfiguration = new ScenarioConfiguration(scenarioId, s.get, numUsers.get, ramp.get, startTime.get, feeder)
  }
  def configureScenario(s: ScenarioBuilder) = new ScenarioConfigurationBuilder(Some(s), Some(500), Some((0, TimeUnit.SECONDS)), Some((0, TimeUnit.SECONDS)), None)
}