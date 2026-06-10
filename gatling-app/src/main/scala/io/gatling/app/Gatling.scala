/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import java.util.concurrent.TimeUnit

import scala.util.control.NonFatal

import io.gatling.app.cli.GatlingArgsParser
import io.gatling.core.actor.ActorSystem
import io.gatling.core.cli.GatlingArgs
import io.gatling.core.config.GatlingConfiguration
import io.gatling.netty.util.Transports

import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

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
  private[gatling] def fromArgs(args: Array[String]): Int = {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    new GatlingArgsParser(args).parseArguments match {
      case Left(gatlingArgs) => start(gatlingArgs)
      case Right(statusCode) => statusCode.code
    }
  }

  private def start(gatlingArgs: GatlingArgs): Int =
    try {
      // [e]
      //
      //
      //
      //
      // [e]

      val configuration = loadConfiguration()
      val runResult = gatlingArgs.reportsOnly match {
        case Some(runId) => new RunResult(runId, hasAssertions = true)
        case _           =>
          // start actor system before creating simulation instance, some components might need it (e.g. shutdown hook)
          val system = new ActorSystem
          val eventLoopGroup = Transports.newEventLoopGroup(configuration.netty.useNativeTransport, configuration.netty.useIoUring, 0, "gatling")
          try {
            val runner = Runner(system, eventLoopGroup, gatlingArgs, configuration)
            runner.run()
          } finally {
            terminateActorSystem(system)
            eventLoopGroup.shutdownGracefully(0, configuration.core.shutdownTimeout, TimeUnit.MILLISECONDS).awaitUninterruptibly()
          }
      }
      new RunResultProcessor(gatlingArgs, configuration).processRunResult(runResult).code
    } catch {
      case lifeCycleException: GatlingLifecycleException =>
        logger.error(lifeCycleException.getMessage, lifeCycleException.getCause)
        throw lifeCycleException.getCause
      case e: Throwable =>
        logger.error("Run crashed", e)
        throw e
    } finally {
      flushLoggers()
    }

  private def loadConfiguration(): GatlingConfiguration =
    GatlingLifecycleException.manage(t => new GatlingLifecycleException.Configuration(t)) {
      GatlingConfiguration.load()
    }

  private def terminateActorSystem(system: ActorSystem): Unit =
    try {
      system.close()
    } catch {
      case NonFatal(e) =>
        logger.debug("Could not terminate ActorSystem", e)
    }

  private def flushLoggers(): Unit = {
    val factory = LoggerFactory.getILoggerFactory
    try {
      factory.getClass.getMethod("stop").invoke(factory)
    } catch {
      case _: NoSuchMethodException => // Fail silently if a logging provider other than LogBack is used.
      case NonFatal(ex)             => logger.warn("Logback failed to shutdown.", ex)
    }
  }
}
