package com.excilys.ebi.gatling.core.scenario

import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder

import java.util.concurrent.CountDownLatch

abstract class ScenarioBuilder extends AbstractActionBuilder {
  def end(latch: CountDownLatch): AbstractActionBuilder
}