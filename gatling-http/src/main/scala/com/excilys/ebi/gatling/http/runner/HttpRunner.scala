package com.excilys.ebi.gatling.http.runner

import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.result.writer.FileDataWriter
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.result.message.ActionInfo

import com.excilys.ebi.gatling.http.context.HttpContext
import com.excilys.ebi.gatling.http.context.builder.HttpContextBuilder._
import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder.HttpScenarioBuilder
import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder
import com.excilys.ebi.gatling.http.action.HttpRequestAction

import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch;
import java.util.Date

import akka.actor.Scheduler
import akka.actor.Actor.actorOf

object HttpRunner {
  class HttpRunner(s: HttpScenarioBuilder, numUsers: Int, ramp: Option[Int]) extends Runner(s, numUsers, ramp) {
    val latch: CountDownLatch = new CountDownLatch(numUsers)
    val scenario = scenarioBuilder.end(latch).build

    logger.info("[{}] Expecting {} relevant actions to be executed in this scenario", s.getName, (s.getNumberOfRelevantActions + 1) * numUsers)
    logger.info("[{}] Simulation execution time will be at least {}s", s.getName, s.getExecutionTime)

    def run = {

      val statWriter = actorOf[FileDataWriter].start

      statWriter ! InitializeDataWriter(new Date, s.getName, (s.getNumberOfRelevantActions + 1) * numUsers)

      logger.debug("Stats Write Actor Uuid: {}", statWriter.getUuid)
      logger.debug("Launching All Scenarios")
      for (i <- 1 to numberOfUsers) {
        //statWriter ! ActionInfo(i, "Beginning of scenario", new Date, 0, "OK")

        ramp.map { time =>
          Scheduler.scheduleOnce(() => scenario.execute(httpContext withUserId i withWriteActorUuid statWriter.getUuid build), (time / numberOfUsers) * i, TimeUnit.MILLISECONDS)
        }.getOrElse {
          scenario.execute(httpContext withUserId i withWriteActorUuid statWriter.getUuid build)
        }
      }
      logger.debug("Finished Launching scenarios executions")
      latch.await(86400, TimeUnit.SECONDS)
      HttpRequestAction.CLIENT.close
      logger.debug("Runner execution ended")
    }

  }

  def play(s: HttpScenarioBuilder, numUsers: Int): Unit = {
    play(s, numUsers, 0)
  }

  def play(s: HttpScenarioBuilder, numUsers: Int, ramp: Int): Unit = {
    new HttpRunner(s, numUsers, Some(ramp)).run
  }
}