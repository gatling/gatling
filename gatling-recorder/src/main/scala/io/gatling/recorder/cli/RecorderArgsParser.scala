/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.core.cli.CliOptionParser
import io.gatling.recorder.render.template.RenderingFormat
import io.gatling.shared.cli.RecorderCliOptions._

private[recorder] final class RecorderArgsParser(args: Array[String]) {
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
