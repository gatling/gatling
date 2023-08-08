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

package io.gatling.bundle

import java.net.{ URI, URL }
import java.nio.file.Path
import java.util.UUID

import io.gatling.bundle.CommandArguments.RunMode
import io.gatling.bundle.commands.CommandHelper

object CommandArguments {
  val Empty: CommandArguments = new CommandArguments(
    commandMode = null,
    packageId = None,
    simulationId = None,
    apiToken = None,
    teamId = None,
    simulationClass = None,
    simulationSystemProperties = Map.empty,
    simulationEnvironmentVariables = Map.empty,
    batchMode = false,
    waitForRunEnd = false,
    url = new URI("https://cloud.gatling.io").toURL,
    runMode = None,
    reportsOnly = None,
    extraJavaOptionsCompile = Nil,
    extraJavaOptionsRun = Nil,
    binariesDirectory = CommandHelper.DefaultBinariesDirectory,
    resourcesDirectory = CommandHelper.DefaultResourcesDirectory,
    confDirectory = CommandHelper.ConfDirectory,
    controlPlaneUrl = None
  )

  sealed abstract class RunMode(val value: String)
  object RunMode {
    case object RunEnterprise extends RunMode("enterprise")
    case object EnterprisePackage extends RunMode("package")
    case object RunLocal extends RunMode("local")
  }
}

final case class CommandArguments(
    commandMode: RunMode,
    packageId: Option[UUID],
    simulationId: Option[UUID],
    apiToken: Option[String],
    teamId: Option[UUID],
    simulationClass: Option[String],
    simulationSystemProperties: Map[String, String],
    simulationEnvironmentVariables: Map[String, String],
    batchMode: Boolean,
    waitForRunEnd: Boolean,
    url: URL,
    runMode: Option[RunMode],
    reportsOnly: Option[String],
    extraJavaOptionsCompile: List[String],
    extraJavaOptionsRun: List[String],
    binariesDirectory: Path,
    resourcesDirectory: Path,
    confDirectory: Path,
    controlPlaneUrl: Option[URL]
)
