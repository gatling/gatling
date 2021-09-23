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

package io.gatling.app

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.scenario.{ Simulation, SimulationParams }

sealed abstract class SimulationClass(clazz: Class[_]) extends Product with Serializable {
  def simpleName: String = clazz.getSimpleName
  def canonicalName: String = clazz.getCanonicalName
  def params(configuration: GatlingConfiguration): SimulationParams
}

object SimulationClass {
  final case class Scala(clazz: Class[Simulation]) extends SimulationClass(clazz) {
    override def params(configuration: GatlingConfiguration): SimulationParams =
      clazz.getDeclaredConstructor().newInstance().params(configuration)
  }
  final case class Java(clazz: Class[JavaSimulation]) extends SimulationClass(clazz) {
    override def params(configuration: GatlingConfiguration): SimulationParams =
      clazz.getDeclaredConstructor().newInstance().params(configuration)
  }
}
