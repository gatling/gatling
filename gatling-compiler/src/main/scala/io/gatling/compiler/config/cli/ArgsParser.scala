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
package io.gatling.compiler.config.cli

import scopt.{ OptionDef, OptionParser, Read }

import io.gatling.compiler.config.cli.CommandLineConstants._

private[config] case class CommandLineOverrides(
  simulationsDirectory: String = "",
  binariesFolder:       String = "",
  classpathElements:    String = ""
)

private[config] class ArgsParser(args: Array[String]) {

  private class CompilerOptionParser extends OptionParser[CommandLineOverrides]("compiler") {
    override def errorOnUnknownArgument: Boolean = false

    def opt[A: Read](constant: CommandLineConstant): OptionDef[A, CommandLineOverrides] =
      opt[A](constant.full).abbr(constant.abbr)
  }

  private val cliOptsParser = new CompilerOptionParser {
    help("help").abbr("h")

    opt[String](SimulationsFolder)
      .action { (folder, c) => c.copy(simulationsDirectory = folder) }

    opt[String](BinariesFolder)
      .action { (binFolder, c) => c.copy(binariesFolder = binFolder) }

    opt[String](CompilerClasspath)
      .action { (classpath, c) => c.copy(classpathElements = classpath) }
  }

  def parseArguments =
    cliOptsParser.parse(args, CommandLineOverrides()).get
}
