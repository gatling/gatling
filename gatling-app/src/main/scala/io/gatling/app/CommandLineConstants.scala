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
package io.gatling.app

object CommandLineConstants {

  object full {
    val Help = "help"
    val NoReports = "no-reports"
    val ReportsOnly = "reports-only"
    val DataFolder = "data-folder"
    val ResultsFolder = "results-folder"
    val RequestBodiesFolder = "request-bodies-folder"
    val SimulationsFolder = "simulations-folder"
    val SimulationsBinariesFolder = "simulations-binaries-folder"
    val Simulation = "simulation"
    val OutputDirectoryBaseName = "output-name"
    val SimulationDescription = "simulation-description"
    val Mute = "mute"
  }

  object short {
    val Help = "h"
    val NoReports = "nr"
    val ReportsOnly = "ro"
    val DataFolder = "df"
    val ResultsFolder = "rf"
    val RequestBodiesFolder = "bf"
    val SimulationsFolder = "sf"
    val SimulationsBinariesFolder = "sbf"
    val Simulation = "s"
    val OutputDirectoryBaseName = "on"
    val SimulationDescription = "sd"
    val Mute = "m"
  }

}
