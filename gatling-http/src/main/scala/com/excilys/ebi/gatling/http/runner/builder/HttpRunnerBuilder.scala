package com.excilys.ebi.gatling.http.runner.builder

import com.excilys.ebi.gatling.http.runner.HttpRunner.HttpRunner

import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder.HttpScenarioBuilder

import java.util.concurrent.TimeUnit

object HttpRunnerBuilder {
  class HttpRunnerBuilder(s: Option[HttpScenarioBuilder], numUsers: Option[Int], ramp: Option[(Int, TimeUnit)]) {
    def withUsersNumber(nbUsers: Int) = new HttpRunnerBuilder(s, Some(nbUsers), ramp)

    def withRamp(rampTime: Int): HttpRunnerBuilder = withRamp(rampTime, TimeUnit.SECONDS)

    def withRamp(rampTime: Int, unit: TimeUnit) = new HttpRunnerBuilder(s, numUsers, Some((rampTime, unit)))

    def play = new HttpRunner(s.get, numUsers.get, ramp).run
  }
  def prepareSimulationFor(s: HttpScenarioBuilder) = new HttpRunnerBuilder(Some(s), None, None)
}