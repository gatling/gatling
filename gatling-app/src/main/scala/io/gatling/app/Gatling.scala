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

package io.gatling.app

import java.nio.file.FileSystems
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal

import io.gatling.app.cli.GatlingArgsParser
import io.gatling.core.cli.GatlingArgs
import io.gatling.core.config.GatlingConfiguration
import io.gatling.netty.util.Transports

import com.typesafe.scalalogging.StrictLogging
import org.apache.pekko.actor.ActorSystem
import org.slf4j.LoggerFactory

object Gatling extends StrictLogging {
  // used by bundle
  def main(args: Array[String]): Unit = {
    System.out.flush()
    // [e]
    //
    // [e]
    sys.exit(fromArgs(args))
  }

  // used by sbt-test-framework
  private[gatling] def fromArgs(args: Array[String]): Int =
    new GatlingArgsParser(args).parseArguments match {
      case Left(gatlingArgs) => start(gatlingArgs)
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

  private def start(gatlingArgs: GatlingArgs) =
    try {
      // [e]
      //
      //
      //
      //
      // [e]

      logger.trace("Starting")
      // workaround for deadlock issue, see https://github.com/gatling/gatling/issues/3411
      FileSystems.getDefault
      val configuration = GatlingConfiguration.load()
      logger.trace("Configuration loaded")
      logger.trace("ActorSystem instantiated")
      val runResult =
        gatlingArgs.reportsOnly match {
          case Some(runId) => new RunResult(runId, hasAssertions = true)
          case _           =>
            // start actor system before creating simulation instance, some components might need it (e.g. shutdown hook)
            val system = ActorSystem("GatlingSystem", GatlingConfiguration.loadActorSystemConfiguration())
            val eventLoopGroup = Transports.newEventLoopGroup(configuration.netty.useNativeTransport, configuration.netty.useIoUring, 0, "gatling")
            try {
              val runner = Runner(system, eventLoopGroup, gatlingArgs, configuration)
              logger.trace("Runner instantiated")
              runner.run()
            } catch {
              case e: Throwable =>
                logger.error("Run crashed", e)
                throw e
            } finally {
              terminateActorSystem(system, configuration.core.shutdownTimeout.milliseconds)
              eventLoopGroup.shutdownGracefully(0, configuration.core.shutdownTimeout, TimeUnit.MILLISECONDS)
            }
        }
      new RunResultProcessor(gatlingArgs, configuration).processRunResult(runResult).code
    } finally {
      val factory = LoggerFactory.getILoggerFactory
      try {
        factory.getClass.getMethod("stop").invoke(factory)
      } catch {
        case _: NoSuchMethodException => // Fail silently if a logging provider other than LogBack is used.
        case NonFatal(ex)             => logger.warn("Logback failed to shutdown.", ex)
      }
    }
}
