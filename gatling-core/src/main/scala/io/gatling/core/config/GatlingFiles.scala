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

package io.gatling.core.config

import java.nio.file.{ Files, Path, Paths }

import scala.util.Properties.{ envOrElse, propOrElse }

object GatlingFiles {
  private val GatlingHome: Path = Paths.get(envOrElse("GATLING_HOME", propOrElse("GATLING_HOME", ".")))

  private[gatling] def resolvePath(path: Path): Path =
    (if (path.isAbsolute || Files.exists(path)) path else GatlingHome.resolve(path)).normalize.toAbsolutePath

  def customResourcesDirectory(configuration: DirectoryConfiguration): Option[Path] = configuration.customResources.map(resolvePath)
  def binariesDirectory(configuration: DirectoryConfiguration): Path =
    configuration.binaries.map(resolvePath).getOrElse(GatlingHome.resolve("target").resolve("test-classes"))
  def resultDirectory(runUuid: String, configuration: DirectoryConfiguration): Path = resolvePath(configuration.results).resolve(runUuid)

  def simulationLogDirectory(runUuid: String, create: Boolean, configuration: DirectoryConfiguration): Path = {
    val dir = resultDirectory(runUuid, configuration)
    if (create) {
      Files.createDirectories(dir)
    } else {
      require(Files.exists(dir), s"simulation directory '$dir' doesn't exist")
      require(Files.isDirectory(dir), s"simulation directory '$dir' is not a directory")
      dir
    }
  }
}
