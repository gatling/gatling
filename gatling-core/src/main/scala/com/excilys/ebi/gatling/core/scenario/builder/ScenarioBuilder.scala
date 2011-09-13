package com.excilys.ebi.gatling.core.scenario.builder

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

  abstract class ScenarioBuilder(val name: String, var actionBuilders: List[AbstractActionBuilder]) extends AbstractActionBuilder {
    def actionsList = actionBuilders
    def getName = name

    def end(latch: CountDownLatch): AbstractActionBuilder
  }
}