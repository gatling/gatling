/**
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import java.nio.file.FileSystems

import scala.concurrent.Await
import scala.concurrent.duration._

import io.gatling.app.cli.ArgsParser
import io.gatling.core.config.GatlingConfiguration

import akka.actor.ActorSystem
import ch.qos.logback.classic.LoggerContext
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory

/**
 * Object containing entry point of application
 */
object Gatling extends StrictLogging {

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

  private[app] def start(overrides: ConfigOverrides, selectedSimulationClass: SelectedSimulationClass) =
    try {
      logger.trace("Starting")
      // workaround for deadlock issue, see https://github.com/gatling/gatling/issues/3411
      FileSystems.getDefault
      val configuration = GatlingConfiguration.load(overrides)
      logger.trace("Configuration loaded")
      // start actor system before creating simulation instance, some components might need it (e.g. shutdown hook)
      val system = ActorSystem("GatlingSystem", GatlingConfiguration.loadActorSystemConfiguration())
      logger.trace("ActorSystem instantiated")
      val runResult =
        try {
          val runner = Runner(system, configuration)
          logger.trace("Runner instantiated")
          runner.run(selectedSimulationClass)
        } finally {
          val whenTerminated = system.terminate()
          Await.result(whenTerminated, 2 seconds)
        }
      RunResultProcessor(configuration).processRunResult(runResult).code
    } finally {
      LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].stop()
    }
}
