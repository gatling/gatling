/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

  private val cliOptsParser = new GatlingOptionParser[Unit]("gatling") {
    opt[Unit](NoReports)
      .foreach(_ => props.noReports())

    opt[String](ReportsOnly)
      .foreach(props.reportsOnly)

    opt[String](ResourcesFolder)
      .foreach(props.resourcesDirectory)

    opt[String](ResultsFolder)
      .foreach(props.resultsDirectory)

    opt[String](BinariesFolder)
      .foreach(props.binariesDirectory)

    opt[String](Simulation)
      .foreach(props.simulationClass)

    opt[String](RunDescription)
      .foreach(props.runDescription)

    opt[String](Launcher)
      .foreach(props.launcher)

    opt[String](BuildToolVersion)
      .foreach(props.buildToolVersion)
  }

  def parseArguments: Either[ConfigOverrides, StatusCode] =
    if (cliOptsParser.parse(args)) Left(props.build)
    else Right(StatusCode.InvalidArguments)
}
