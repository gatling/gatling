package com.excilys.ebi.gatling.http.runner

import com.excilys.ebi.gatling.core.runner.Runner
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.result.writer.FileDataWriter
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.context.builder.ContextBuilder._
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder
import com.excilys.ebi.gatling.core.scenario.configuration.builder.ScenarioConfigurationBuilder._
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfiguration

import com.excilys.ebi.gatling.http.action.HttpRequestAction

import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch;
import java.util.Date

import akka.actor.Scheduler
import akka.actor.Actor.actorOf

import org.apache.commons.lang3.time.FastDateFormat

object HttpRunner {
  class HttpRunner(startDate: Date, configurationBuilders: List[ScenarioConfigurationBuilder]) extends Runner(startDate, configurationBuilders) {

    var totalNumberOfUsers = 0
    var totalNumberOfRelevantActions = 0
    var scenarioConfigurations: List[ScenarioConfiguration] = Nil
    var scenarios: List[Action] = Nil
    val statWriter = actorOf[FileDataWriter].start

    for (i <- 1 to scenarioConfigurationBuilders.size)
      scenarioConfigurations = scenarioConfigurationBuilders(i - 1).build(i) :: scenarioConfigurations

    for (configuration <- scenarioConfigurations)
      totalNumberOfUsers += configuration.numberOfUsers

    val latch: CountDownLatch = new CountDownLatch(totalNumberOfUsers)

    for (configuration <- scenarioConfigurations) {
      scenarios = configuration.scenarioBuilder.end(latch).build(configuration.scenarioId) :: scenarios
      totalNumberOfRelevantActions += configuration.numberOfRelevantActions
    }

    val scenariosAndConfigurations = scenarioConfigurations zip scenarios.reverse

    logger.debug("[Runner] {} requests to be executed for this simulation.", totalNumberOfRelevantActions)
    logger.info("total number of users : {}", totalNumberOfUsers)
    logger.debug("Map of relevant actions: {}", ScenarioBuilder.getNumberOfRelevantActionsByScenario)
    logger.debug("Number of relevant actions for the simulation: {}", totalNumberOfRelevantActions)
    // TODO
    // logger.info("[Runner] Simulation execution time will be at least {}s", ScenarioBuilder.getExecutionTime + TimeUnit.SECONDS.convert(ramp.map { r => r._1.toLong }.getOrElse(0L), ramp.map { r => r._2 }.getOrElse(TimeUnit.SECONDS)))

    def run = {

      statWriter ! InitializeDataWriter(startDate, totalNumberOfRelevantActions)

      logger.debug("Launching All Scenarios")

      for (scenarioAndConfiguration <- scenariosAndConfigurations) {
        val startTime = scenarioAndConfiguration._1.startTime
        Scheduler.scheduleOnce(() => executeOneScenario(scenarioAndConfiguration._1, scenarioAndConfiguration._2), startTime._1, startTime._2)
      }

      logger.debug("Finished Launching scenarios executions")
      latch.await(86400, TimeUnit.SECONDS)
      logger.debug("Latch is at 0")
      HttpRequestAction.CLIENT.close
    }

    def executeOneScenario(configuration: ScenarioConfiguration, scenario: Action) = {

      if (configuration.numberOfUsers == 1) {
        // if only 1 user, execute right now
        val context = buildContext(configuration, 1)
        scenario.execute(context)

      } else {
        // otherwise, schedule
        val ramp = configuration.ramp
        // compute ramp period in millis so we can ramp less that one user per second
        val period = ramp._2.toMillis(ramp._1) / (configuration.numberOfUsers - 1)

        for (i <- 1 to configuration.numberOfUsers) {
          val context: Context = buildContext(configuration, i)
          Scheduler.scheduleOnce(() => scenario.execute(context), period * (i - 1), TimeUnit.MILLISECONDS)
        }
      }
    }

    def buildContext(configuration: ScenarioConfiguration, i: Int) = {
      val ctx = newContext withUserId i withWriteActorUuid statWriter.getUuid withScenarioName configuration.scenarioBuilder.getName build

      configuration.feeder.map { f => ctx.setAttributes(f.next) }

      ctx
    }
  }

  def runSim(startDate: Date)(scenarioConfigurations: ScenarioConfigurationBuilder*) = new HttpRunner(startDate, scenarioConfigurations.toList).run
}