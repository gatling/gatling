/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import java.net.URL
import java.util.UUID

import io.gatling.bundle.CommandArguments.RunMode

object CommandArguments {
  val Empty: CommandArguments = new CommandArguments(
    commandMode = null,
    packageId = None,
    simulationId = None,
    apiToken = None,
    teamId = None,
    simulationClass = None,
    simulationSystemProperties = Map.empty,
    batchMode = false,
    url = new URL("https://cloud.gatling.io"),
    runMode = None,
    reportsOnly = None
  )

  sealed abstract class RunMode(val value: String)
  case object RunLocal extends RunMode("local")
  case object RunEnterprise extends RunMode("enterprise")
}

final case class CommandArguments(
    commandMode: RunMode,
    packageId: Option[UUID],
    simulationId: Option[UUID],
    apiToken: Option[String],
    teamId: Option[UUID],
    simulationClass: Option[String],
    simulationSystemProperties: Map[String, String],
    batchMode: Boolean,
    url: URL,
    runMode: Option[RunMode],
    reportsOnly: Option[String]
) {
  def getApiUrl: URL = new URL(url.toExternalForm + "/api/public")
}
