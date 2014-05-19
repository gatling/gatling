/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
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

  def mute(): Unit =
    props += core.Mute -> true

  def noReports(): Unit =
    props += charting.NoReports -> true

  def reportsOnly(v: String): Unit =
    props += core.directory.ReportsOnly -> v

  def dataDirectory(v: String): Unit =
    props += core.directory.Data -> v

  def resultsDirectory(v: String): Unit =
    props += core.directory.Results -> v

  def requestBodiesDirectory(v: String): Unit =
    props += core.directory.RequestBodies -> v

  def sourcesDirectory(v: String): Unit =
    props += core.directory.Simulations -> v

  def binariesDirectory(v: String): Unit =
    props += core.directory.Binaries -> v

  def simulationClass(v: String): Unit =
    props += core.SimulationClass -> v

  def outputDirectoryBaseName(v: String): Unit =
    props += core.OutputDirectoryBaseName -> v

  def runDescription(v: String): Unit =
    props += core.RunDescription -> v

  def disableCompiler(): Unit =
    props += core.DisableCompiler -> true

  def build = props
}
