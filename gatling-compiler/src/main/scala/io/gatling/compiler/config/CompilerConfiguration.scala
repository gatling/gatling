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
package io.gatling.compiler.config

import java.io.File
import java.nio.file._

import scala.collection.JavaConversions.mapAsJavaMap

import com.typesafe.config.ConfigFactory

import io.gatling.compiler.config.ConfigUtils._
import io.gatling.compiler.config.cli.{ CommandLineOverrides, ArgsParser }

case class CompilerConfiguration(
  encoding: String,
  simulationsDirectory: Path,
  binariesDirectory: Path,
  classesDirectory: Path,
  classpathElements: Seq[File])

object CompilerConfiguration {

  private val encodingKey = "gatling.core.encoding"
  private val simulationsDirectoryKey = "gatling.core.directory.simulations"
  private val binariesDirectoryKey = "gatling.core.directory.binaries"

  def configuration(args: Array[String]) = {
      def buildConfigurationMap(overrides: CommandLineOverrides): Map[String, _ <: Any] = {
        val mapForSimulationFolder =
          string2option(overrides.simulationsDirectory)
            .map(v => Map(simulationsDirectoryKey -> v))
            .getOrElse(Map.empty)

        mapForSimulationFolder
      }

    val argsParser = new ArgsParser(args)
    val commandLineOverrides = argsParser.parseArguments

    val cliConfig = ConfigFactory.parseMap(buildConfigurationMap(commandLineOverrides))
    val customConfig = ConfigFactory.load("gatling.conf")
    val defaultConfig = ConfigFactory.load("gatling-defaults.conf")
    val config = cliConfig.withFallback(customConfig.withFallback(defaultConfig))

    val encoding = config.getString(encodingKey)
    val simulationsDirectory = resolvePath(Paths.get(config.getString(simulationsDirectoryKey)))
    val binariesDirectory = string2option(config.getString(binariesDirectoryKey)).map(Paths.get(_)).getOrElse(GatlingHome.resolve("target"))
    val classesDirectory = binariesDirectory.resolve("test-classes")
    val classpathElements = commandLineOverrides.classpathElements.split(File.pathSeparator).map(new File(_))

    CompilerConfiguration(encoding, simulationsDirectory, binariesDirectory, classesDirectory, classpathElements)
  }

}
