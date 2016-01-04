/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.core.controller.throttle

import io.gatling.core.scenario.SimulationParams

import akka.actor.{ Props, ActorSystem, ActorRef }

case class Throttles(global: Option[Throttle], perScenario: Map[String, Throttle]) {

  def limitReached(scenario: String) = {
    global.map(_.limitReached) match {
      case Some(true) => true
      case _          => perScenario.collectFirst { case (`scenario`, throttle) => throttle.limitReached }.getOrElse(false)
    }
  }

  def increment(scenario: String) = {
    global.foreach(_.increment())
    perScenario.get(scenario).foreach(_.increment())
  }
}

class Throttle(val limit: Int, var count: Int = 0) {

  def increment(): Unit = count += 1

  def limitReached: Boolean = count >= limit

  override def toString = s"Throttle(limit=$limit, count=$count)"
}

object Throttler {

  val ThrottlerActorName = "gatling-throttler"
  val ThrottlerControllerActorName = "gatling-throttler-controller"

  def apply(system: ActorSystem, simulationParams: SimulationParams) = {

    val throttler = system.actorOf(Props(new ThrottlerActor), ThrottlerActorName)
    val throttlerController = system.actorOf(Props(new ThrottlerController(throttler, simulationParams.throttlings)), ThrottlerControllerActorName)
    new Throttler(throttlerController, throttler)
  }
}

class Throttler(throttlerController: ActorRef, throttlerActor: ActorRef) {

  def start(): Unit = throttlerController ! ThrottlerControllerCommand.Start

  def throttle(scenarioName: String, action: () => Unit): Unit =
    throttlerActor ! ThrottledRequest(scenarioName, action)
}
