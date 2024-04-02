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

package io.gatling.recorder.cli

import java.nio.file.Path

import io.gatling.core.cli.{ CliOption, CliOptionParser }
import io.gatling.recorder.render.template.RenderingFormat

object RecorderArgsParser {
  private val SimulationsFolder: CliOption = new CliOption(
    "simulations-folder",
    "sf",
    "Uses <directoryPath> as the absolute path of the directory where simulations are stored",
    Some("<directoryPath>")
  )
  private val ResourcesFolder: CliOption =
    new CliOption("resources-folder", "rf", "Uses <folderName> as the folder where generated resources will be stored", Some("<folderName>"))
  private val Package: CliOption = new CliOption("package", "pkg", "Sets the package of the generated class", Some("<package>"))
  private val ClassName: CliOption = new CliOption("classname", "cn", "Sets the name of the generated class", Some("<classname>"))
  private val Format: CliOption = new CliOption("format", "fmt", "Sets the format of the generated code", Some("<format>"))
}

private[recorder] final class RecorderArgsParser(args: Array[String]) {

  import RecorderArgsParser._

  private var simulationsFolder: Option[Path] = None
  private var resourcesFolder: Option[Path] = None
  private var pkg: Option[String] = None
  private var className: Option[String] = None
  private var format: Option[RenderingFormat] = None

  private val cliOptsParser = new CliOptionParser[Unit]("recorder") {
    opt[String](SimulationsFolder)
      .foreach(v => simulationsFolder = Some(Path.of(v)))

    opt[String](ResourcesFolder)
      .foreach(v => resourcesFolder = Some(Path.of(v)))

    opt[String](Package)
      .foreach(v => pkg = Some(v))

    opt[String](ClassName)
      .foreach(v => className = Some(v))

    opt[String](Format)
      .foreach(v => format = Some(RenderingFormat.fromString(v)))
  }

  def parseArguments: RecorderArgs =
    if (cliOptsParser.parse(args)) {
      RecorderArgs(
        simulationsFolder = simulationsFolder.getOrElse(throw new IllegalArgumentException("Missing simulationsFolder")),
        resourcesFolder = resourcesFolder.getOrElse(throw new IllegalArgumentException("Missing resourcesFolder")),
        pkg = pkg,
        className = className,
        format = format
      )
    } else {
      throw new IllegalArgumentException(s"Failed to parse args ${args.mkString}")
    }
}
