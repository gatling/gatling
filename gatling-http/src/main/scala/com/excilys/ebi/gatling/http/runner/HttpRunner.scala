package com.excilys.ebi.gatling.http.runner

import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.Action

import com.excilys.ebi.gatling.http.context.HttpContext
import com.excilys.ebi.gatling.http.context.builder.HttpContextBuilder._
import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder.HttpScenarioBuilder
import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder

import java.util.concurrent.TimeUnit

import akka.actor.Scheduler

object HttpRunner {
  class HttpRunner(s: HttpScenarioBuilder, numUsers: Integer, ramp: Option[Integer]) extends Runner(s.build, numUsers, ramp) {

    logger.info("[{}] Expecting {} relevant actions to be executed in this scenario", s.getName, s.getNumberOfRelevantActions * numUsers)

    def run = {
      for (i <- 1 to numberOfUsers) {
        ramp match {
          case Some(time) => Scheduler.scheduleOnce(() => scenario.execute(httpContext withUserId i build), (time / numberOfUsers) * i, TimeUnit.MILLISECONDS);
          case None => scenario.execute(httpContext withUserId i build)
        }
      }
    }

  }

  def play(s: HttpScenarioBuilder, numUsers: Integer): Unit = {
    play(s, numUsers, 0)
  }

  def play(s: HttpScenarioBuilder, numUsers: Integer, ramp: Integer): Unit = {
    new HttpRunner(s, numUsers, Some(ramp)).run
  }
}