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
package io.gatling.app.cli

import io.gatling.app.ConfigOverrides
import io.gatling.app.cli.CommandLineConstants._
import io.gatling.core.cli.GatlingOptionParser
import io.gatling.core.config.GatlingPropertiesBuilder

private[app] class ArgsParser(args: Array[String]) {

  private val props = new GatlingPropertiesBuilder

  private val cliOptsParser = new GatlingOptionParser("gatling") {

    help(Help).text("Show help (this message) and exit")

    opt[Unit](NoReports)
      .foreach(_ => props.noReports())
      .text("Runs simulation but does not generate reports")

    opt[Unit](Mute)
      .foreach(_ => props.mute())
      .text("Runs in mute mode: doesn't ask for run description or simulation ID, uses defaults")

    opt[String](ReportsOnly)
      .foreach(props.reportsOnly)
      .valueName("<directoryName>")
      .text("Generates the reports for the simulation in <directoryName>")

    opt[String](DataFolder)
      .foreach(props.dataDirectory)
      .valueName("<directoryPath>")
      .text("Uses <directoryPath> as the absolute path of the directory where feeders are stored")

    opt[String](ResultsFolder)
      .foreach(props.resultsDirectory)
      .valueName("<directoryPath>")
      .text("Uses <directoryPath> as the absolute path of the directory where results are stored")

    opt[String](BodiesFolder)
      .foreach(props.bodiesDirectory)
      .valueName("<directoryPath>")
      .text("Uses <directoryPath> as the absolute path of the directory where bodies are stored")

    opt[String](SimulationsFolder)
      .foreach(props.sourcesDirectory)
      .valueName("<directoryPath>")
      .text("Uses <directoryPath> to discover simulations that could be run")

    opt[String](BinariesFolder)
      .foreach(props.binariesDirectory)
      .valueName("<directoryPath>")
      .text("Uses <directoryPath> as the absolute path of the directory where Gatling should produce compiled binaries")

    opt[String](Simulation)
      .foreach(props.simulationClass)
      .valueName("<className>")
      .text("Runs <className> simulation")

    opt[String](OutputDirectoryBaseName)
      .foreach(props.outputDirectoryBaseName)
      .valueName("<name>")
      .text("Use <name> for the base name of the output directory")

    opt[String](SimulationDescription)
      .foreach(props.runDescription)
      .valueName("<description>")
      .text("A short <description> of the run to include in the report")
  }

  def parseArguments: Either[ConfigOverrides, StatusCode] =
    if (cliOptsParser.parse(args)) Left(props.build)
    else Right(StatusCode.InvalidArguments)
}
