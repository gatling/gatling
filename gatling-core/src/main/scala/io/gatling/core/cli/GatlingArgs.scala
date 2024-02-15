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

package io.gatling.core.cli

import java.nio.file.Path

object GatlingArgs {
  val Empty: GatlingArgs = GatlingArgs(
    simulationClass = None,
    runDescription = None,
    noReports = false,
    reportsOnly = None,
    resultsDirectory = None,
    launcher = None,
    buildToolVersion = None
  )
}
final case class GatlingArgs(
    simulationClass: Option[String],
    runDescription: Option[String],
    noReports: Boolean,
    reportsOnly: Option[String],
    resultsDirectory: Option[Path],
    launcher: Option[String],
    buildToolVersion: Option[String]
)
