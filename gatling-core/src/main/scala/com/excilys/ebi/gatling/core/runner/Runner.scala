/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.runner

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.writer.FileDataWriter
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.context.builder.ContextBuilder._
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder
import com.excilys.ebi.gatling.core.scenario.configuration.builder.ScenarioConfigurationBuilder
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfiguration
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.Date
import akka.actor.Scheduler
import akka.actor.Actor.actorOf
import akka.actor.Actor.registry
import org.apache.commons.lang3.time.FastDateFormat
import org.joda.time.DateTime
import com.excilys.ebi.gatling.core.scenario.Scenario

abstract class Runner(val startDate: DateTime, val scenarioConfigurationBuilders: List[ScenarioConfigurationBuilder], val onFinished: () => Unit) extends Logging {
  var totalNumberOfUsers = 0
  var scenarioConfigurations: List[ScenarioConfiguration] = Nil
  var scenarios: List[Scenario] = Nil
  val statWriter = actorOf[FileDataWriter].start

  // stores all scenario configurations
  for (i <- 1 to scenarioConfigurationBuilders.size)
    scenarioConfigurations = scenarioConfigurationBuilders(i - 1).build(i) :: scenarioConfigurations

  // Counts the number of users
  for (configuration <- scenarioConfigurations)
    totalNumberOfUsers += configuration.users

  // Initializes a countdown latch to determine when to stop the application
  val latch: CountDownLatch = new CountDownLatch(totalNumberOfUsers + 1)

  // Builds all scenarios
  for (configuration <- scenarioConfigurations) {
    scenarios = configuration.scenarioBuilder.end(latch).build :: scenarios
  }

  // Creates a List of Tuples with scenario configuration / scenario 
  val scenariosAndConfigurations = scenarioConfigurations zip scenarios.reverse

  logger.info("Total number of users : {}", totalNumberOfUsers)

  /**
   * This method schedules the beginning of all scenarios
   */
  def run = {

    // Initilization of the data writer
    statWriter ! InitializeDataWriter(startDate, latch)

    logger.debug("Launching All Scenarios")

    // Scheduling all scenarios
    for (scenarioAndConfiguration <- scenariosAndConfigurations) {
      val delay = scenarioAndConfiguration._1.delay
      Scheduler.scheduleOnce(() => {
        logger.debug("Launching Scenario: {}", scenarioAndConfiguration._2.getFirstAction)
        startOneScenario(scenarioAndConfiguration._1, scenarioAndConfiguration._2.getFirstAction)
      }, delay._1, delay._2)
    }

    logger.debug("Finished Launching scenarios executions")
    latch.await(86400, TimeUnit.SECONDS)

    logger.debug("All scenarios finished, stoping actors")
    // Shuts down all actors
    registry.shutdownAll

    // Executes the onFinished function
    onFinished
  }

  /**
   * This method starts one scenario
   *
   * @param configuration the configuration of the scenario
   * @scenario the scenario that will be executed
   * @return Nothing
   */
  private def startOneScenario(configuration: ScenarioConfiguration, scenario: Action) = {
    if (configuration.users == 1) {
      // if only 1 user, execute right now
      val context = buildContext(configuration, 1)
      scenario.execute(context)
    } else {
      // otherwise, schedule
      val ramp = configuration.ramp
      // compute ramp period in millis so we can ramp less that one user per second
      val period = ramp._2.toMillis(ramp._1) / (configuration.users - 1)

      for (i <- 1 to configuration.users) {
        val context: Context = buildContext(configuration, i)
        Scheduler.scheduleOnce(() => scenario.execute(context), period * (i - 1), TimeUnit.MILLISECONDS)
      }
    }
  }

  /**
   * This method builds the context that will be sent to the first action of a scenario
   *
   * @param configuration the configuration of the scenario
   * @param userId the id of the current user
   * @return the built context
   */
  private def buildContext(configuration: ScenarioConfiguration, userId: Int) = {
    val ctx = newContext withUserId userId withWriteActorUuid statWriter.getUuid withScenarioName configuration.scenarioBuilder.getName build

    // Puts all values of one line of the feeder in the context
    configuration.feeder.map { f => ctx.setAttributes(f.next) }

    ctx
  }
}