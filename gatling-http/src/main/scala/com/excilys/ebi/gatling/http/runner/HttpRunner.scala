package com.excilys.ebi.gatling.http.runner

import com.excilys.ebi.gatling.core.feeder.Feeder
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
  class HttpRunner(s: HttpScenarioBuilder, numUsers: Int, ramp: Option[Int], feeder: Option[Feeder]) extends Runner(s, numUsers, ramp, feeder) {
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
        val context: HttpContext = feeder.map { f =>
          logger.debug("Context With FeederIndex")
          httpContext withUserId i withWriteActorUuid statWriter.getUuid withFeederIndex f.nextIndex build
        }.getOrElse {
          httpContext withUserId i withWriteActorUuid statWriter.getUuid build
        }

        ramp.map { time =>
          Scheduler.scheduleOnce(() => scenario.execute(context), (time / numberOfUsers) * i, TimeUnit.MILLISECONDS)
        }.getOrElse {
          scenario.execute(context)
        }
      }
      logger.debug("Finished Launching scenarios executions")
      latch.await(86400, TimeUnit.SECONDS)
      HttpRequestAction.CLIENT.close
      logger.debug("Runner execution ended")
    }

  }
}