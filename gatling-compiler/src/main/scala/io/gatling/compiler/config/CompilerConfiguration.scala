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
package io.gatling.compiler.config

import java.io.File
import java.nio.file._

import scala.collection.JavaConversions.mapAsJavaMap

import io.gatling.compiler.config.ConfigUtils._
import io.gatling.compiler.config.cli.{ CommandLineOverrides, ArgsParser }

import com.typesafe.config.ConfigFactory

private[compiler] case class CompilerConfiguration(
  encoding:             String,
  simulationsDirectory: Path,
  binariesDirectory:    Path,
  classpathElements:    Seq[File]
)

private[compiler] object CompilerConfiguration {

  private val encodingKey = "gatling.core.encoding"
  private val simulationsDirectoryKey = "gatling.core.directory.simulations"
  private val binariesDirectoryKey = "gatling.core.directory.binaries"

  def configuration(args: Array[String]) = {
      def buildConfigurationMap(overrides: CommandLineOverrides): Map[String, _ <: Any] = {
        val mapForSimulationFolder =
          string2option(overrides.simulationsDirectory)
            .map(v => Map(simulationsDirectoryKey -> v))
            .getOrElse(Map.empty)

        val mapForBinariesFolder =
          string2option(overrides.binariesFolder)
            .map(v => Map(binariesDirectoryKey -> v))
            .getOrElse(Map.empty)

        mapForSimulationFolder ++ mapForBinariesFolder
      }

    val argsParser = new ArgsParser(args)
    val commandLineOverrides = argsParser.parseArguments

    val cliConfig = ConfigFactory.parseMap(buildConfigurationMap(commandLineOverrides))
    val customConfig = ConfigFactory.load("gatling.conf")
    val defaultConfig = ConfigFactory.load("gatling-defaults.conf")
    val config = cliConfig.withFallback(customConfig.withFallback(defaultConfig))

    val encoding = config.getString(encodingKey)
    val simulationsDirectory = resolvePath(Paths.get(config.getString(simulationsDirectoryKey)))
    val binariesDirectory = string2option(config.getString(binariesDirectoryKey)).map(path => resolvePath(path)).getOrElse(GatlingHome / "target" / "test-classes")
    val classpathElements = commandLineOverrides.classpathElements.split(File.pathSeparator).map(new File(_))

    CompilerConfiguration(encoding, simulationsDirectory, binariesDirectory, classpathElements)
  }

}
