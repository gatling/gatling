package com.excilys.ebi.gatling.http.runner.builder

import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.http.runner.HttpRunner.HttpRunner

import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder.HttpScenarioBuilder

object HttpRunnerBuilder {
  class HttpRunnerBuilder(s: Option[HttpScenarioBuilder], numUsers: Option[Int], ramp: Option[Int], feeder: Option[Feeder]) {
    def withUsersNumber(nbUsers: Int) = new HttpRunnerBuilder(s, Some(nbUsers), ramp, feeder)

    def withRamp(ramp: Int) = new HttpRunnerBuilder(s, numUsers, Some(ramp), feeder)

    def withFeeder(feeder: Feeder) = new HttpRunnerBuilder(s, numUsers, ramp, Some(feeder))

    def play = new HttpRunner(s.get, numUsers.get, ramp, feeder).run
  }
  def prepareSimulationFor(s: HttpScenarioBuilder) = new HttpRunnerBuilder(Some(s), None, None, None)
}