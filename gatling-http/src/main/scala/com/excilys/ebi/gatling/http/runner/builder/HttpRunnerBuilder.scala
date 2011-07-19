package com.excilys.ebi.gatling.http.runner.builder

import com.excilys.ebi.gatling.http.runner.HttpRunner.HttpRunner

import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder.HttpScenarioBuilder

object HttpRunnerBuilder {
  class HttpRunnerBuilder(s: Option[HttpScenarioBuilder], numUsers: Option[Int], ramp: Option[Int]) {
    def withUsersNumber(nbUsers: Int) = new HttpRunnerBuilder(s, Some(nbUsers), ramp)

    def withRamp(ramp: Int) = new HttpRunnerBuilder(s, numUsers, Some(ramp))

    def play = new HttpRunner(s.get, numUsers.get, ramp).run
  }
  def prepareSimulationFor(s: HttpScenarioBuilder) = new HttpRunnerBuilder(Some(s), None, None)
}