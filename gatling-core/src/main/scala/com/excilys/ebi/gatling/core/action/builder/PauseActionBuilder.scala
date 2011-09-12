package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.PauseAction
import com.excilys.ebi.gatling.core.action.Action

import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder

import java.util.concurrent.TimeUnit

import akka.actor.TypedActor

object PauseActionBuilder {
  class PauseActionBuilder(val duration: Option[Int], val timeUnit: Option[TimeUnit], val next: Option[Action]) extends AbstractActionBuilder {

    def withDuration(duration: Int) = new PauseActionBuilder(Some(duration), timeUnit, next)

    def withTimeUnit(timeUnit: TimeUnit) = new PauseActionBuilder(duration, Some(timeUnit), next)

    def withNext(next: Action) = new PauseActionBuilder(duration, timeUnit, Some(next))

    def build(scenarioId: Int): Action = {
      logger.debug("Building PauseAction with duration: {}ms", TimeUnit.MILLISECONDS.convert(duration.get, timeUnit.get))
      ScenarioBuilder.addToExecutionTime(scenarioId, duration.get, timeUnit.get)
      TypedActor.newInstance(classOf[Action], new PauseAction(next.get, duration.get, timeUnit.get))
    }
  }

  def pauseActionBuilder = new PauseActionBuilder(None, Some(TimeUnit.SECONDS), None)

}