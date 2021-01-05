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

package io.gatling.recorder.ui.headless

import java.io.{ File, PrintStream }
import java.lang.management.ManagementFactory

import scala.util.Using

import io.gatling.recorder.config.{ RecorderConfiguration, RecorderMode }
import io.gatling.recorder.config.RecorderMode.Proxy
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.ui.{ FrontEndEvent, RecorderFrontEnd }

private[headless] object HeadlessFrontEnd {
  private val RecorderPidFile = new File(".gatling-recorder-pid")
}
private[ui] class HeadlessFrontEnd(controller: RecorderController)(implicit configuration: RecorderConfiguration) extends RecorderFrontEnd(controller) {

  import HeadlessFrontEnd._

  private var hasRun = false

  override def selectedRecorderMode: RecorderMode = configuration.core.mode

  override def receiveEvent(event: FrontEndEvent): Unit = println(s"[Event] $event")

  override def init(): Unit =
    if (!hasRun) {
      hasRun = true
      println("Starting Recorder in headless mode")
      if (selectedRecorderMode == Proxy && RecorderPidFile.exists()) {
        printErr(s"Recorder lock file found at $RecorderPidFile.")
        printErr("Make sure that there is no other recording in progress.")
        sys.exit(1)
      } else startRecording()
    } else sys.runtime.halt(0)

  override def handleHarExportFailure(message: String): Unit =
    printErr(s"Could not convert HAR file: $message")

  override def harFilePath: String = configuration.core.harFilePath.getOrElse("")

  override def handleHarExportSuccess(): Unit =
    println("HAR file successfully converted.")

  override def recordingStarted(): Unit = {
    createLockFile()
    println(s"Recording started, proxy port is ${configuration.proxy.port}")
    println("To stop the Recorder and generate the Simulation, kill the Recorder process with: ")
    println("- CTRL-C")
    println(s"- Use the Recorder's PID, written to $RecorderPidFile")
    sys.addShutdownHook(stopRecording(true))
  }

  override def handleFilterValidationFailures(failures: Seq[String]): Unit = ()

  override def askSimulationOverwrite: Boolean = {
    printErr("Another simulation with the same name exists.")
    false
  }

  override def recordingStopped(): Unit = {
    RecorderPidFile.delete()
    println("New Gatling simulation created.")
  }

  override def handleMissingHarFile(path: String): Unit = {
    val errorMessage = {
      if (path.isEmpty) "The HAR file to convert was not specified, either through recorder.conf or through CLI options."
      else s"Could not find the HAR file (path: $path)"
    }
    printErr(errorMessage)
  }

  private def createLockFile(): Unit = {
    val pid = ManagementFactory.getRuntimeMXBean.getName.split("@").head
    Using.resource(new PrintStream(RecorderPidFile))(_.println(pid))
  }

  private def printErr(msg: String): Unit =
    Console.err.println(msg)
}
