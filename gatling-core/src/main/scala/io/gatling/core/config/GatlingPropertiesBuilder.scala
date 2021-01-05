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

package io.gatling.core.config

import scala.collection.mutable

import io.gatling.core.ConfigKeys._

class GatlingPropertiesBuilder {

  private val props = mutable.Map.empty[String, Any]

  def noReports(): GatlingPropertiesBuilder = {
    props += charting.NoReports -> true
    this
  }

  def reportsOnly(v: String): GatlingPropertiesBuilder = {
    props += core.directory.ReportsOnly -> v
    this
  }

  def resourcesDirectory(v: String): GatlingPropertiesBuilder = {
    props += core.directory.Resources -> v
    this
  }

  def resultsDirectory(v: String): GatlingPropertiesBuilder = {
    props += core.directory.Results -> v
    this
  }

  def simulationsDirectory(v: String): GatlingPropertiesBuilder = {
    props += core.directory.Simulations -> v
    this
  }

  def binariesDirectory(v: String): GatlingPropertiesBuilder = {
    props += core.directory.Binaries -> v
    this
  }

  def simulationClass(v: String): GatlingPropertiesBuilder = {
    props += core.SimulationClass -> v
    this
  }

  def runDescription(v: String): GatlingPropertiesBuilder = {
    props += core.RunDescription -> v
    this
  }

  def build: mutable.Map[String, Any] = props
}
