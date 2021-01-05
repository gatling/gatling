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

package io.gatling.sbt

import java.io.{ PrintWriter, StringWriter }

import io.gatling.app.Gatling
import io.gatling.app.cli.StatusCode
import io.gatling.core.scenario.Simulation

import sbt.testing.{ EventHandler, Logger, OptionalThrowable, Task, TaskDef, TestSelector }

/**
 * The main worker of the test framework :
 * <ul>
 *   <li>Loads the simulation from the test ClassLoader.</li>
 *   <li>Run Gatling with the specified simulation.</li>
 *   <li>Fire the appropriate event, depending on the outcome of the run.</li>
 * </ul>
 * @param taskDef the selected simulation metadata
 * @param testClassLoader the test ClassLoader, provided by SBT.
 * @param args the arguments for the new run
 * @param remoteArgs the arguments for the run in a forked JVM
 */
class GatlingTask(val taskDef: TaskDef, testClassLoader: ClassLoader, args: Array[String], remoteArgs: Array[String]) extends Task {

  override val tags: Array[String] = Array.empty

  def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[Task] = {
    // Load class
    val className = taskDef.fullyQualifiedName
    val simulationClass = testClassLoader.loadClass(className).asInstanceOf[Class[Simulation]]

    // Start Gatling and compute duration
    val before = System.nanoTime()
    val (returnCode, exception) =
      try {
        (Gatling.fromArgs(args, Some(simulationClass)), None)
      } catch {
        case e: Exception =>
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          loggers.foreach(_.error(sw.toString))
          (StatusCode.AssertionsFailed.code, Some(e))
      }
    val duration = (System.nanoTime() - before) / 1000

    // Prepare event data
    val simulationName = simulationClass.getSimpleName
    val selector = new TestSelector(simulationName)
    val optionalThrowable = exception.map(new OptionalThrowable(_)).getOrElse(new OptionalThrowable)
    val fingerprint = taskDef.fingerprint

    // Check return code and fire appropriate event
    @SuppressWarnings(Array("org.wartremover.warts.Product", "org.wartremover.warts.Serializable"))
    // problem's in sbt
    val event = returnCode match {
      case StatusCode.Success.code =>
        loggers.foreach(_.info(s"Simulation $simulationName successful."))
        SimulationSuccessful(className, fingerprint, selector, optionalThrowable, duration)

      case StatusCode.AssertionsFailed.code =>
        loggers.foreach(_.error(s"Simulation $simulationName failed."))
        SimulationFailed(className, fingerprint, selector, optionalThrowable, duration)

      case StatusCode.InvalidArguments.code =>
        val formattedArgs = args.mkString("(", "", ")")
        loggers.foreach(_.error(s"Provided arguments $formattedArgs are not valid."))
        InvalidArguments(className, fingerprint, selector, optionalThrowable, duration)
    }

    eventHandler.handle(event)

    // No new task to launch
    Array.empty[Task]
  }
}
