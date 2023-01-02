/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.bundle

import io.gatling.core.cli.CommandLineConstant

private[bundle] object CLIHelper {
  private def matchOption(arg: String, option: CommandLineConstant): Boolean =
    arg == s"-${option.abbr}" || arg == s"--${option.full}"

  def filterArgOptions(args: List[String], options: List[CommandLineConstant]): List[String] = {
    val argsIndexes = args.toIndexedSeq
    options
      .foldLeft(IndexedSeq[String]()) { case (acc, option) =>
        acc ++ (for {
          i <- argsIndexes.indices
          value = argsIndexes(i)
          if matchOption(value, option) || (option.valueName.nonEmpty && i > 0 && matchOption(argsIndexes(i - 1), option))
        } yield value)
      }
      .toList
  }
}
