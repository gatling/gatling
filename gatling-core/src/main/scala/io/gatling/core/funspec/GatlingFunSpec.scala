package io.gatling.core.funspec

import io.gatling.core.Predef._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.Protocol
import io.gatling.core.structure.ChainBuilder

import scala.collection.mutable.ListBuffer

abstract class GatlingFunSpec extends Simulation {

  /** Set the protocol configuration used by gatling to execute all specs in this class */
  def protocolConf: Protocol

  /** Add a spec to be executed */
  def spec(actionBuilder: ActionBuilder) = specs += actionBuilder

  private[this] val specs = new ListBuffer[ActionBuilder]

  private[this] lazy val testScenario = scenario(this.getClass.getSimpleName)
    .exec(ChainBuilder(specs.reverse.toList))

  private[core] def setupRegisteredSpecs = {
    require(specs.length > 0, "At least one spec needs to be defined")
    setUp(testScenario.inject(atOnceUsers(1)))
      .protocols(protocolConf)
      .assertions(forAll.failedRequests.percent.is(0))
  }

}
