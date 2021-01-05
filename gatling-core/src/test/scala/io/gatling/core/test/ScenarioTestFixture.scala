/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.test

import java.util.concurrent.ConcurrentLinkedDeque

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import io.gatling.BaseSpec
import io.gatling.commons.util.DefaultClock
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.pause.Constant
import io.gatling.core.protocol.ProtocolComponentsRegistries
import io.gatling.core.structure._

import akka.actor.{ ActorRef, ActorSystem }
import io.netty.channel.EventLoopGroup

final case class ScenarioTestContext(scenarioContext: ScenarioContext, statsEngine: LoggingStatsEngine, exitAction: BlockingExitAction) {

  private[test] val expectations = new ArrayBuffer[PartialFunction[Any, Unit]]
}

trait ScenarioTestFixture extends BaseSpec {

  def configuration: GatlingConfiguration

  private def resolve(msgQueue: ConcurrentLinkedDeque[Any], expectations: ArrayBuffer[PartialFunction[Any, Unit]]): Unit = {
    val msgIt = msgQueue.iterator
    expectations.zipWithIndex.foreach { case (expectation, i) =>
      if (!msgIt.hasNext) {
        throw new AssertionError(s"Expectation $i didn't receive any message")
      }
      val msg = msgIt.next()

      if (!expectation.isDefinedAt(msg)) {
        throw new AssertionError(s"Expectation $i didn't match message $msg")
      }

      expectation(msg)
    }

    if (msgIt.hasNext) {
      throw new AssertionError(s"Unmatched received messages ${msgIt.asScala.toVector}")
    }
  }

  def scenarioTest(f: ScenarioTestContext => Unit): Unit = {
    val system = ActorSystem.create()

    try {
      val statsEngine = new LoggingStatsEngine
      val coreComponents =
        new CoreComponents(system, mock[EventLoopGroup], mock[ActorRef], None, statsEngine, new DefaultClock, mock[Action], configuration)
      val protocolComponentsRegistry = new ProtocolComponentsRegistries(coreComponents, Map.empty).scenarioRegistry(Map.empty)
      val scenarioContext = new ScenarioContext(coreComponents, protocolComponentsRegistry, Constant, throttled = false)
      val exitAction = new BlockingExitAction(1)
      val ctx = ScenarioTestContext(scenarioContext, statsEngine, exitAction)

      f(ctx)
      exitAction.await(2.seconds)
      resolve(statsEngine.msgQueue, ctx.expectations)

    } finally {
      Await.ready(system.terminate(), 2.seconds)
    }
  }

  def buildChain(chainBuilder: ChainBuilder)(implicit cxt: ScenarioTestContext): Action =
    chainBuilder.build(cxt.scenarioContext, cxt.exitAction)

  def logMsg(msg: Any)(implicit ctx: ScenarioTestContext): Unit =
    ctx.statsEngine.msgQueue.addLast(msg)

  def expectMsg(msg: Any)(implicit ctx: ScenarioTestContext): Unit =
    ctx.expectations += { case m => m shouldBe msg }

  def expectMsgPF(f: PartialFunction[Any, Unit])(implicit ctx: ScenarioTestContext): Unit =
    ctx.expectations += f
}
