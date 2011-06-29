package com.excilys.ebi.gatling.core.scenario

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ScenarioBuilder {
  private var expectedExecutiontime: Long = 0

  def addToExecutionTime(timeValue: Int, timeUnit: TimeUnit) = {
    expectedExecutiontime += TimeUnit.SECONDS.convert(timeValue, timeUnit)
  }
}
abstract class ScenarioBuilder extends AbstractActionBuilder {
  def getExecutionTime = ScenarioBuilder.expectedExecutiontime
  def end(latch: CountDownLatch): AbstractActionBuilder
}