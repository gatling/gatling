/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal

import io.gatling.app.cli.ArgsParser
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.scenario.Simulation
import io.gatling.netty.util.Transports

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory

object Gatling extends StrictLogging {

  // used by bundle
  def main(args: Array[String]): Unit = sys.exit(fromArgs(args, None))

  // used by maven archetype
  def fromMap(overrides: ConfigOverrides): Int = start(overrides, None)

  // used by sbt-test-framework
  private[gatling] def fromSbtTestFramework(args: Array[String], selectedSimulationClass: Class[Simulation]): Int =
    fromArgs(args, Some(SimulationClass.Scala(selectedSimulationClass)))

  private def fromArgs(args: Array[String], forcedSimulationClass: Option[SimulationClass]): Int =
    new ArgsParser(args).parseArguments match {
      case Left(overrides)   => start(overrides, forcedSimulationClass)
      case Right(statusCode) => statusCode.code
    }

  private def terminateActorSystem(system: ActorSystem, timeout: FiniteDuration): Unit =
    try {
      val whenTerminated = system.terminate()
      Await.result(whenTerminated, timeout)
    } catch {
      case NonFatal(e) =>
        logger.debug("Could not terminate ActorSystem", e)
    }

  private def start(overrides: ConfigOverrides, forcedSimulationClass: Option[SimulationClass]) =
    try {
      //[fl]
      //
      //
      //
      //
      //[fl]

      logger.trace("Starting")
      // workaround for deadlock issue, see https://github.com/gatling/gatling/issues/3411
      FileSystems.getDefault
      val configuration = GatlingConfiguration.load(overrides)
      logger.trace("Configuration loaded")
      logger.trace("ActorSystem instantiated")
      val runResult =
        configuration.core.directory.reportsOnly match {
          case Some(runId) => new RunResult(runId, hasAssertions = true)
          case _           =>
            // start actor system before creating simulation instance, some components might need it (e.g. shutdown hook)
            val system = ActorSystem("GatlingSystem", GatlingConfiguration.loadActorSystemConfiguration())
            val eventLoopGroup = Transports.newEventLoopGroup(configuration.netty.useNativeTransport, 0, "gatling")
            try {
              val runner = Runner(system, eventLoopGroup, configuration)
              logger.trace("Runner instantiated")
              runner.run(forcedSimulationClass)
            } catch {
              case e: Throwable =>
                logger.error("Run crashed", e)
                throw e
            } finally {
              eventLoopGroup.shutdownGracefully(0, configuration.core.shutdownTimeout, TimeUnit.MILLISECONDS)
              terminateActorSystem(system, configuration.core.shutdownTimeout.milliseconds)
            }
        }
      new RunResultProcessor(configuration).processRunResult(runResult).code
    } finally {
      val factory = LoggerFactory.getILoggerFactory
      try {
        factory.getClass.getMethod("stop").invoke(factory)
      } catch {
        case _: NoSuchMethodException => //Fail silently if a logging provider other than LogBack is used.
        case NonFatal(ex)             => logger.warn("Logback failed to shutdown.", ex)
      }
    }
}
