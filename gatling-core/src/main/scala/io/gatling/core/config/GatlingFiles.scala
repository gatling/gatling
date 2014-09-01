/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.config

import java.io.File
import java.net.URI

import scala.tools.nsc.io.{ Directory, Path }
import scala.tools.nsc.io.Path.string2path
import scala.util.Properties.{ envOrElse, propOrElse }

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.UriHelper._

object GatlingFiles {

  val GatlingHome: URI = pathToUri(envOrElse("GATLING_HOME", propOrElse("GATLING_HOME", ".")))
  val GatlingAssetsPackage = "assets"
  val GatlingJsFolder = "js"
  val GatlingStyleFolder = "style"
  val GatlingAssetsJsPackage = GatlingAssetsPackage / GatlingJsFolder
  val GatlingAssetsStylePackage = GatlingAssetsPackage / GatlingStyleFolder

  private def resolvePath(path: String): URI = {
    val rawPath = Path(path)
    if (rawPath.isAbsolute || rawPath.exists) path.toURI else GatlingHome / path
  }

  def dataDirectory: URI = resolvePath(configuration.core.directory.data)
  def requestBodiesDirectory: URI = resolvePath(configuration.core.directory.requestBodies)
  def sourcesDirectory: URI = resolvePath(configuration.core.directory.sources)
  def reportsOnlyDirectory: Option[String] = configuration.core.directory.reportsOnly
  def binariesDirectory: Option[URI] = configuration.core.directory.binaries.map(new File(_).toURI)
  def resultDirectory(runUuid: String): URI = resolvePath(configuration.core.directory.results) / runUuid
  def jsDirectory(runUuid: String): URI = resultDirectory(runUuid) / GatlingJsFolder
  def styleDirectory(runUuid: String): URI = resultDirectory(runUuid) / GatlingStyleFolder

  def simulationLogDirectory(runUuid: String, create: Boolean = true): URI = {
    val dir = resultDirectory(runUuid)
    if (create) {
      dir.toFile.mkdirs()
      // FIXME : clean it up, currently necessary to ensure that it is a dir URI
      resultDirectory(runUuid)
    } else {
      require(dir.toFile.exists, s"simulation directory '$dir' doesn't exist")
      require(dir.toFile.isDirectory, s"simulation directory '$dir' is not a directory")
      dir
    }
  }
}
