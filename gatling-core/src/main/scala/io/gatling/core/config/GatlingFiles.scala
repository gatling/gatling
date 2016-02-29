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
package io.gatling.core.config

import java.nio.file.Path

import scala.util.Properties.{ envOrElse, propOrElse }

import io.gatling.commons.util.PathHelper._

object GatlingFiles {

  val GatlingHome: Path = envOrElse("GATLING_HOME", propOrElse("GATLING_HOME", "."))
  val GatlingAssetsPackage: Path = "assets"
  val GatlingJsFolder: Path = "js"
  val GatlingStyleFolder: Path = "style"
  val GatlingAssetsJsPackage: Path = GatlingAssetsPackage / GatlingJsFolder
  val GatlingAssetsStylePackage: Path = GatlingAssetsPackage / GatlingStyleFolder

  private def resolvePath(path: Path): Path =
    (if (path.isAbsolute || path.exists) path else GatlingHome / path).normalize().toAbsolutePath

  def dataDirectory(implicit configuration: GatlingConfiguration): Path = resolvePath(configuration.core.directory.data)
  def bodiesDirectory(implicit configuration: GatlingConfiguration): Path = resolvePath(configuration.core.directory.bodies)
  def sourcesDirectory(implicit configuration: GatlingConfiguration): Path = resolvePath(configuration.core.directory.sources)
  def reportsOnlyDirectory(implicit configuration: GatlingConfiguration): Option[String] = configuration.core.directory.reportsOnly
  def binariesDirectory(configuration: GatlingConfiguration): Path = configuration.core.directory.binaries.map(path => resolvePath(path)).getOrElse(GatlingHome / "target" / "test-classes")
  def resultDirectory(runUuid: String)(implicit configuration: GatlingConfiguration): Path = resolvePath(configuration.core.directory.results) / runUuid
  def jsDirectory(runUuid: String)(implicit configuration: GatlingConfiguration): Path = resultDirectory(runUuid) / GatlingJsFolder
  def styleDirectory(runUuid: String)(implicit configuration: GatlingConfiguration): Path = resultDirectory(runUuid) / GatlingStyleFolder

  def simulationLogDirectory(runUuid: String, create: Boolean = true)(implicit configuration: GatlingConfiguration): Path = {
    val dir = resultDirectory(runUuid)
    if (create) dir.mkdirs
    else {
      require(dir.toFile.exists, s"simulation directory '$dir' doesn't exist")
      require(dir.toFile.isDirectory, s"simulation directory '$dir' is not a directory")
      dir
    }
  }
}
