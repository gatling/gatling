/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.core.controller.inject

import java.util.concurrent.{ LinkedBlockingQueue, TimeUnit }

import scala.concurrent.duration._

import io.gatling.commons.util.DefaultClock
import io.gatling.core.action.Action
import io.gatling.core.actor.{ ActorRef, ActorSpec }
import io.gatling.core.controller.Controller
import io.gatling.core.controller.inject.PopulationFlows.TopDownNode
import io.gatling.core.controller.inject.closed.{ ClosedInjectionProfile, ConstantConcurrentUsersInjection }
import io.gatling.core.controller.inject.open.{ AtOnceOpenInjection, NothingForOpenInjection, OpenInjectionProfile }
import io.gatling.core.scenario.{ Population, Scenario }
import io.gatling.core.session.Session
import io.gatling.core.stats.LoggingStatsEngine

import io.netty.channel.DefaultEventLoopGroup

class InjectorSpec extends ActorSpec {
  // so huge that the ticks never happen during the tests: everything must happen without them
  private val NoTick = 1.hour

  private val eventLoopGroup = new DefaultEventLoopGroup(1)

  override protected def afterAll(): Unit = {
    eventLoopGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS)
    super.afterAll()
  }

  private final class Run {
    val injector: ActorRef[Injector.Command] =
      actorSystem.actorOf(Injector.actor(eventLoopGroup, new LoggingStatsEngine, new DefaultClock, NoTick))
    val controller = mockActorRef[Controller.Command]("controller")
    private val startedUsers = new LinkedBlockingQueue[String]

    private val entry: Action = new Action {
      override def name: String = "entry"
      override protected def execute(session: Session): Unit = {
        startedUsers.add(session.scenario)
        session.eventLoop.schedule((() => injector ! Injector.Command.UserEnd(session.scenario)): Runnable, 50, TimeUnit.MILLISECONDS)
      }
    }

    def population(name: String, profile: InjectionProfile): Population =
      Population(new Scenario(name, entry, identity, _ => (), null), profile, shard = false)

    def start(nodes: TopDownNode[String, Population]*): Unit =
      injector ! Injector.Command.Start(controller, PopulationFlows.fromTopDownNodes(nodes.toList))

    def expectUserStarted(scenario: String): Unit =
      startedUsers.poll(5, TimeUnit.SECONDS) shouldBe scenario

    def expectRunCompleted(): Unit =
      controller.expectMsgType[Controller.Command.StopLoadGenerator](5.seconds).reason shouldBe Controller.Command.StopLoadGenerator.Reason.Graceful.Completed
  }

  private def atOnce(users: Int): InjectionProfile = new OpenInjectionProfile(List(AtOnceOpenInjection(users)))

  private def node(population: Population, children: TopDownNode[String, Population]*): TopDownNode[String, Population] =
    TopDownNode(population.scenario.name, population, if (children.isEmpty) Nil else List(children.toList))

  "Injector" should "complete the run when a root scenario without children completes" in {
    val run = new Run
    run.start(node(run.population("A", atOnce(1))))

    run.expectUserStarted("A")
    run.expectRunCompleted()
  }

  it should "start a child scenario as soon as its parent completes, without waiting for the next tick" in {
    val run = new Run
    run.start(node(run.population("A", atOnce(1)), node(run.population("B", atOnce(1)))))

    run.expectUserStarted("A")
    run.expectUserStarted("B")
    run.expectRunCompleted()
  }

  it should "chain through a scenario with an empty injection profile without waiting for the next tick" in {
    val run = new Run
    run.start(
      node(
        run.population("A", atOnce(1)),
        node(run.population("Empty", new OpenInjectionProfile(List(NothingForOpenInjection(Duration.Zero)))), node(run.population("B", atOnce(1))))
      )
    )

    run.expectUserStarted("A")
    run.expectUserStarted("B")
    run.expectRunCompleted()
  }

  it should "start a closed workload child scenario as soon as its parent completes and complete it at the end of its duration" in {
    val run = new Run
    run.start(
      node(
        run.population("A", atOnce(1)),
        node(run.population("B", new ClosedInjectionProfile(List(ConstantConcurrentUsersInjection(1, 200.millis)))))
      )
    )

    run.expectUserStarted("A")
    run.expectUserStarted("B")
    run.expectRunCompleted()
  }
}
