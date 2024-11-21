/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Base64

import io.gatling.commons.util.StringHelper._
import io.gatling.core.cli.{ CliOptionParser, GatlingArgs }
import io.gatling.shared.cli.GatlingCliOptions._

private[app] final class GatlingArgsParser(args: Array[String]) {
  private var gatlingArgs = GatlingArgs.Empty

  private val cliOptsParser = new CliOptionParser[Unit]("gatling") {
    opt[Unit](NoReports)
      .foreach(_ => gatlingArgs = gatlingArgs.copy(noReports = true))

    opt[String](ReportsOnly)
      .foreach(value => gatlingArgs = gatlingArgs.copy(reportsOnly = value.trimToOption))

    opt[String](ResultsFolder)
      .foreach(value => gatlingArgs = gatlingArgs.copy(resultsDirectory = value.trimToOption.map(trimmed => Paths.get(trimmed))))

    opt[String](Simulation)
      .foreach(value => gatlingArgs = gatlingArgs.copy(simulationClass = value.trimToOption))

    opt[String](RunDescription)
      .foreach(value => gatlingArgs = gatlingArgs.copy(runDescription = value.trimToOption.map(tryDecodeBase64)))

    opt[String](Launcher)
      .foreach(value => gatlingArgs = gatlingArgs.copy(launcher = value.trimToOption))

    opt[String](BuildToolVersion)
      .foreach(value => gatlingArgs = gatlingArgs.copy(buildToolVersion = value.trimToOption))
  }

  private def tryDecodeBase64(raw: String): String =
    try {
      new String(Base64.getDecoder.decode(raw), StandardCharsets.UTF_8)
    } catch {
      case _: IllegalArgumentException => raw
    }

  def parseArguments: Either[GatlingArgs, StatusCode] =
    if (cliOptsParser.parse(args)) {
      Left(gatlingArgs)
    } else {
      Right(StatusCode.InvalidArguments)
    }
}
