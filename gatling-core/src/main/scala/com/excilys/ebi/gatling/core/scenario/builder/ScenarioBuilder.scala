package com.excilys.ebi.gatling.core.scenario.builder

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.PauseActionBuilder._
import com.excilys.ebi.gatling.core.action.builder.EndActionBuilder._

object ScenarioBuilder {
  private var expectedExecutionDuration: Map[Int, Long] = Map.empty
  private var numberOfRelevantActionsByScenario: Map[Int, Int] = Map.empty

  def addRelevantAction(scenarioId: Int) = {
    numberOfRelevantActionsByScenario += (scenarioId -> (1 + numberOfRelevantActionsByScenario.get(scenarioId).getOrElse(0)))
  }

  def addToExecutionTime(scenarioId: Int, timeValue: Long, timeUnit: TimeUnit) = {
    expectedExecutionDuration += (scenarioId -> (TimeUnit.MILLISECONDS.convert(timeValue, timeUnit) + expectedExecutionDuration.get(scenarioId).getOrElse(0L)))
  }

  def getNumberOfRelevantActionsByScenario = numberOfRelevantActionsByScenario
  def getExecutionTime(scenarioId: Int) = TimeUnit.SECONDS.convert(expectedExecutionDuration.get(scenarioId).get, TimeUnit.MILLISECONDS)

  abstract class ScenarioBuilder[B <: ScenarioBuilder[B]](name: String, actionBuilders: List[AbstractActionBuilder]) extends AbstractActionBuilder {
    def actionsList = actionBuilders
    def getName = name

    def newInstance(name: String, actionBuilders: List[AbstractActionBuilder]): B

    def pause(delayValue: Int): B = {
      pause(delayValue, TimeUnit.SECONDS)
    }

    def pause(delayValue: Int, delayUnit: TimeUnit): B = {
      val pause = pauseActionBuilder withDuration delayValue withTimeUnit delayUnit
      logger.debug("Adding PauseAction")
      newInstance(name, pause :: actionBuilders)
    }

    def iterate(times: Int, chain: B): B = {
      val chainActions: List[AbstractActionBuilder] = chain.actionsList
      var iteratedActions: List[AbstractActionBuilder] = Nil
      for (i <- 1 to times) {
        iteratedActions = chainActions ::: iteratedActions
      }
      logger.debug("Adding {} Iterations", times)
      newInstance(name, iteratedActions ::: actionBuilders)
    }

    def end(latch: CountDownLatch): B = {
      logger.debug("Adding EndAction")
      newInstance(name, endActionBuilder(latch) :: actionBuilders)
    }

    def build(scenarioId: Int): Action = {
      var previousInList: Action = null
      for (actionBuilder <- actionBuilders) {
        previousInList = actionBuilder withNext (previousInList) build (scenarioId)
      }
      println(previousInList)
      previousInList
    }

    def withNext(next: Action) = null
  }
}