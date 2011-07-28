package com.excilys.ebi.gatling.http.runner

import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.result.writer.FileDataWriter
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.context.builder.ContextBuilder._
import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder.HttpScenarioBuilder
import com.excilys.ebi.gatling.http.scenario.HttpScenarioBuilder
import com.excilys.ebi.gatling.http.action.HttpRequestAction

import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch;
import java.util.Date

import akka.actor.Scheduler
import akka.actor.Actor.actorOf

import org.apache.commons.lang.time.FastDateFormat

object HttpRunner {
  class HttpRunner(s: HttpScenarioBuilder, numUsers: Int, ramp: Option[(Int, TimeUnit)]) extends Runner(s, numUsers, ramp) {
    val latch: CountDownLatch = new CountDownLatch(numUsers)
    val scenario = scenarioBuilder.end(latch).build

    logger.info("[{}] Simulation execution time will be at least {}s", s.getName, s.getExecutionTime + TimeUnit.SECONDS.convert(ramp.map { r => r._1.toLong }.getOrElse(0L), ramp.map { r => r._2 }.getOrElse(TimeUnit.SECONDS)))

    def run: String = {

      val statWriter = actorOf[FileDataWriter].start

      val startDate = new Date

      statWriter ! InitializeDataWriter(startDate, s.getName, (s.getNumberOfRelevantActions + 1) * numUsers)

      logger.debug("Launching All Scenarios")
      for (i <- 1 to numberOfUsers) {
        val context: Context = makeContext withUserId i withWriteActorUuid statWriter.getUuid build

        ramp.map { r =>
          Scheduler.scheduleOnce(() => scenario.execute(context), (r._1.toDouble / (numberOfUsers - 1).toDouble).toInt * (i - 1), r._2)
        }.getOrElse {
          scenario.execute(context)
        }
      }
      logger.debug("Finished Launching scenarios executions")
      latch.await(86400, TimeUnit.SECONDS)
      HttpRequestAction.CLIENT.close
      FastDateFormat.getInstance("yyyyMMddhhmmss").format(startDate)
    }

  }
}