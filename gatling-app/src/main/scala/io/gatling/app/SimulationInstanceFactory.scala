/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.app

import io.gatling.core.scenario.Simulation

sealed trait SimulationInstanceFactory {

  def name(): String

  def canonicalName(): String

  def simpleName: String

  def instance(): Simulation

}

final class SimulationInstanceClass(clazz: Class[Simulation]) extends SimulationInstanceFactory {

  override def name(): String = clazz.getName
  override def canonicalName(): String = clazz.getCanonicalName
  override def simpleName: String = clazz.getSimpleName
  override def instance(): Simulation = clazz.getDeclaredConstructor().newInstance()
}

final class ConcreteSimulationInstance(simulation: Simulation) extends SimulationInstanceFactory {

  override def name(): String = simulation.getClass.getName
  override def canonicalName(): String = simulation.getClass.getCanonicalName
  override def simpleName: String = simulation.getClass.getSimpleName
  override def instance(): Simulation = simulation
}
