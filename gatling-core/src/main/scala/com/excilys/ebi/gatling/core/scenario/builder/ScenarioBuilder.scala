package com.excilys.ebi.gatling.core.scenario.builder

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ScenarioBuilder {
  private var expectedExecutiontime: Map[Int, Long] = Map.empty
  private var numberOfRelevantActions: Map[Int, Int] = Map.empty

  def addRelevantAction(scenarioId: Int) = {
    numberOfRelevantActions += (scenarioId -> (1 + numberOfRelevantActions.get(scenarioId).getOrElse(0)))
  }

  def addToExecutionTime(scenarioId: Int, timeValue: Int, timeUnit: TimeUnit) = {
    expectedExecutiontime += (scenarioId -> (TimeUnit.MILLISECONDS.convert(timeValue, timeUnit) + expectedExecutiontime.get(scenarioId).getOrElse(0L)))
  }

  def getNumberOfRelevantActionsMap = numberOfRelevantActions
  def getExecutionTime(scenarioId: Int) = TimeUnit.SECONDS.convert(expectedExecutiontime.get(scenarioId).get, TimeUnit.MILLISECONDS)

  abstract class ScenarioBuilder(val name: String, var actionBuilders: List[AbstractActionBuilder]) extends AbstractActionBuilder {
    def actionsList = actionBuilders
    def getName = name

    def end(latch: CountDownLatch): AbstractActionBuilder
  }
}