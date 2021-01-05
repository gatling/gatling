/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.nio.file._

import scala.jdk.CollectionConverters._

import io.gatling.compiler.config.ConfigUtils._
import io.gatling.compiler.config.cli.{ ArgsParser, CommandLineOverrides }

import com.typesafe.config.ConfigFactory

private[compiler] final case class CompilerConfiguration(
    encoding: String,
    simulationsDirectory: Path,
    binariesDirectory: Path,
    extraScalacOptions: Seq[String]
)

private[compiler] object CompilerConfiguration {

  private val encodingKey = "gatling.core.encoding"
  private val simulationsDirectoryKey = "gatling.core.directory.simulations"
  private val binariesDirectoryKey = "gatling.core.directory.binaries"

  def configuration(args: Array[String]): CompilerConfiguration = {
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

    val cliConfig = ConfigFactory.parseMap(buildConfigurationMap(commandLineOverrides).asJava)
    val customConfig = ConfigFactory.load("gatling.conf")
    val defaultConfig = ConfigFactory.load("gatling-defaults.conf")
    val config = cliConfig.withFallback(customConfig.withFallback(defaultConfig))

    val encoding = config.getString(encodingKey)
    val simulationsDirectory = resolvePath(Paths.get(config.getString(simulationsDirectoryKey)))
    val binariesDirectory = string2option(config.getString(binariesDirectoryKey))
      .fold(GatlingHome / "target" / "test-classes")(resolvePath(_))
    val extraScalacOptions = commandLineOverrides.extraScalacOptions.split(",").toSeq

    CompilerConfiguration(encoding, simulationsDirectory, binariesDirectory, extraScalacOptions)
  }
}
