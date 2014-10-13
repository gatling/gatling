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
package io.gatling.compiler

import java.nio.file._
import scala.util.Properties.{ envOrElse, propOrElse }

import com.typesafe.config.ConfigFactory

object CompilerConfiguration {

  private val encodingKey = "gatling.core.encoding"
  private val sourceDirectoryKey = "gatling.core.directory.simulations"
  private val binariesDirectoryKey = "gatling.core.directory.binaries"

  private def resolvePath(path: Path): Path = {
    if (path.isAbsolute || Files.exists(path)) path else GatlingHome.resolve(path)
  }

  def string2option(string: String) = string.trim match {
    case "" => None
    case s  => Some(s)
  }

  private val configuration = {
    val customConfig = ConfigFactory.load("gatling.conf")
    val defaultConfig = ConfigFactory.load("gatling-defaults.conf")
    customConfig.withFallback(defaultConfig)
  }

  val GatlingHome = Paths.get(envOrElse("GATLING_HOME", propOrElse("GATLING_HOME", ".")))
  val encoding = configuration.getString(encodingKey)
  val sourceDirectory = resolvePath(Paths.get(configuration.getString(sourceDirectoryKey)))
  val binariesDirectory = string2option(configuration.getString(binariesDirectoryKey)).map(Paths.get(_)).getOrElse(GatlingHome.resolve("target"))
  val classesDirectory = binariesDirectory.resolve("test-classes")
}
