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
package io.gatling.app

import io.gatling.app.cli.ArgsParser
import io.gatling.core.config.GatlingConfiguration

/**
 * Object containing entry point of application
 */
object Gatling {

  // used by bundle
  def main(args: Array[String]): Unit = sys.exit(fromArgs(args, None))

  // used by maven archetype
  def fromMap(overrides: ConfigOverrides): Int = start(overrides, None)

  // used by sbt-test-framework
  private[gatling] def fromArgs(args: Array[String], selectedSimulationClass: SelectedSimulationClass): Int =
    new ArgsParser(args).parseArguments match {
      case Left(overrides)   => start(overrides, selectedSimulationClass)
      case Right(statusCode) => statusCode.code
    }

  private[app] def start(overrides: ConfigOverrides, selectedSimulationClass: SelectedSimulationClass) = {
    val configuration = GatlingConfiguration.load(overrides)
    val runResult = Runner(configuration).run(selectedSimulationClass)
    RunResultProcessor(configuration).processRunResult(runResult).code
  }
}
