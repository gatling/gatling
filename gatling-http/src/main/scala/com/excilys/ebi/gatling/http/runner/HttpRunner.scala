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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HttpRunner {
  val LOGGER: Logger = LoggerFactory.getLogger(classOf[HttpRunner])

  class HttpRunner(s: Action, numUsers: Integer, ramp: Option[Integer], numberOfRelevantActions: Integer) extends Runner(s, numUsers, ramp, numberOfRelevantActions) {

    LOGGER.info("Expecting relevant {} actions to be executed during this simulation", numberOfRelevantActions)

    def run = {
      for (i <- 1 to numberOfUsers) {
        ramp match {
          case Some(time) => Scheduler.scheduleOnce(() => s.execute(httpContext withUserId i build), (time / numberOfUsers) * i, TimeUnit.MILLISECONDS);
          case None => s.execute(httpContext withUserId i build)
        }
      }
    }

  }

  def play(s: HttpScenarioBuilder, numUsers: Integer): Unit = {
    play(s, numUsers, 0)
  }

  def play(s: HttpScenarioBuilder, numUsers: Integer, ramp: Integer): Unit = {
    new HttpRunner(s.build, numUsers, Some(ramp), s.getNumberOfRelevantActions * numUsers).run
  }
}