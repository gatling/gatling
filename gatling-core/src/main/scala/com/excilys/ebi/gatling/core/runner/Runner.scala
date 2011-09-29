package com.excilys.ebi.gatling.core.runner

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.writer.FileDataWriter
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.context.builder.ContextBuilder._
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder
import com.excilys.ebi.gatling.core.scenario.configuration.builder.ScenarioConfigurationBuilder._
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfiguration
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.Date
import akka.actor.Scheduler
import akka.actor.Actor.actorOf
import akka.actor.Actor.registry
import org.apache.commons.lang3.time.FastDateFormat
import org.joda.time.DateTime

abstract class Runner(val startDate: DateTime, val scenarioConfigurationBuilders: List[ScenarioConfigurationBuilder], val onFinished: () => Unit) extends Logging {
  var totalNumberOfUsers = 0
  var scenarioConfigurations: List[ScenarioConfiguration] = Nil
  var scenarios: List[Action] = Nil
  val statWriter = actorOf[FileDataWriter].start

  for (i <- 1 to scenarioConfigurationBuilders.size)
    scenarioConfigurations = scenarioConfigurationBuilders(i - 1).build(i) :: scenarioConfigurations

  for (configuration <- scenarioConfigurations)
    totalNumberOfUsers += configuration.numberOfUsers

  val latch: CountDownLatch = new CountDownLatch(totalNumberOfUsers + 1)

  for (configuration <- scenarioConfigurations) {
    scenarios = configuration.scenarioBuilder.end(latch).build(configuration.scenarioId) :: scenarios
  }

  val scenariosAndConfigurations = scenarioConfigurations zip scenarios.reverse

  logger.info("Total number of users : {}", totalNumberOfUsers)

  def run = {

    statWriter ! InitializeDataWriter(startDate, totalNumberOfUsers, latch)

    logger.debug("Launching All Scenarios")

    for (scenarioAndConfiguration <- scenariosAndConfigurations) {
      val startTime = scenarioAndConfiguration._1.startTime
      Scheduler.scheduleOnce(() => executeOneScenario(scenarioAndConfiguration._1, scenarioAndConfiguration._2), startTime._1, startTime._2)
    }

    logger.debug("Finished Launching scenarios executions")
    latch.await(86400, TimeUnit.SECONDS)
    logger.debug("Latch is at 0")

    logger.debug("All scenarios finished, stoping actors")
    registry.shutdownAll

    onFinished
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