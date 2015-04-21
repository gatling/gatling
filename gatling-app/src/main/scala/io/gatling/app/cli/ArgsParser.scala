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
package io.gatling.app.cli

import scopt.{ OptionDef, OptionParser, Read }

import io.gatling.app.{ ConfigOverrides, GatlingStatusCodes, StatusCode }
import io.gatling.app.cli.CommandLineConstants._
import io.gatling.core.config.GatlingPropertiesBuilder

private[app] class ArgsParser(args: Array[String]) {

  private class GatlingOptionParser extends OptionParser[Unit]("gatling") {
    def help(constant: CommandLineConstant): OptionDef[Unit, Unit] =
      help(constant.full).abbr(constant.abbr)

    def opt[A: Read](constant: CommandLineConstant): OptionDef[A, Unit] =
      opt[A](constant.full).abbr(constant.abbr)
  }

  private val props = new GatlingPropertiesBuilder

  private val cliOptsParser = new GatlingOptionParser {

    help(Help).text("Show help (this message) and exit")

    opt[Unit](NoReports)
      .foreach(_ => props.noReports())
      .text("Runs simulation but does not generate reports")

    opt[Unit](Mute)
      .foreach(_ => props.mute())
      .text("Runs in mute mode: don't asks for run description nor simulation ID, use defaults")

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
      .foreach(props.sourcesDirectory)
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
    else Right(GatlingStatusCodes.InvalidArguments)
}
