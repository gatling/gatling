/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder

import io.gatling.recorder.CommandLineConstants._
import io.gatling.recorder.config.{ ConfigKeys, RecorderPropertiesBuilder }
import io.gatling.recorder.controller.RecorderController
import scopt.OptionParser

import java.io.{ File, FileOutputStream }
import java.lang.management.ManagementFactory

object GatlingRecorder {

  private val props = new RecorderPropertiesBuilder

  private val cliOptsParser = new OptionParser[Unit]("gatling-recorder") with CommandLineConstantsSupport[Unit] {
    help(Help).text("Show help (this message) and exit")
    opt[Int](LocalPort).valueName("<port>").foreach(props.localPort).text("Local port used by Gatling Proxy for HTTP/HTTPS")
    opt[String](ProxyHost).valueName("<host>").foreach(props.proxyHost).text("Outgoing proxy host")
    opt[Int](ProxyPort).valueName("<port>").foreach(props.proxyPort).text("Outgoing proxy port for HTTP")
    opt[Int](ProxyPortSsl).valueName("<port>").foreach(props.proxySslPort).text("Outgoing proxy port for HTTPS")
    opt[String](OutputFolder).valueName("<folderName>").foreach(props.simulationOutputFolder).text("Uses <folderName> as the folder where generated simulations will be stored")
    opt[String](RequestBodiesFolder).valueName("<folderName>").foreach(props.requestBodiesFolder).text("Uses <folderName> as the folder where request bodies are stored")
    opt[String](ClassName).foreach(props.simulationClassName).text("Sets the name of the generated class")
    opt[String](Package).foreach(props.simulationPackage).text("Sets the package of the generated class")
    opt[String](Encoding).foreach(props.encoding).text("Sets the encoding used in the recorder")
    opt[Boolean](FollowRedirect).foreach(props.followRedirect).text("""Sets the "Follow Redirects" option to true""")
    opt[Boolean](AutomaticReferer).foreach(props.automaticReferer).text("""Sets the "Automatic Referers" option to true""")
    opt[Boolean](InferHtmlResources).foreach(props.inferHtmlResources).text("""Sets the "Fetch html resources" option to true""")
    opt[Boolean](Headless).foreach(props.runHeadless).text("""Runs the recorder in headless mode""")
  }

  def main(args: Array[String]): Unit = {
    val parsedOptsSuccessfully = cliOptsParser.parse(args)

    if (parsedOptsSuccessfully) {
      val builtProperties = props.build
      log(s"builtProperties: ${builtProperties}")
      val recorderController = RecorderController(builtProperties)
      if (builtProperties.contains(ConfigKeys.core.RunHeadless)
        && builtProperties(ConfigKeys.core.RunHeadless) == true) {
        runHeadless(recorderController)
      }
    }
  }

  def runHeadless(recorderController: RecorderController): Unit = {
    log("Welcome to headless mode.")

    val recordingLockFile = new File(".gatling-recording-in-progress.lock")
    if (recordingLockFile.exists) {
      log(s"Recording lock file already exists: ${recordingLockFile.getAbsolutePath}")
      log("Gatling cannot start recording in headless mode until this file is removed.")
      sys exit 1
    }

    sys addShutdownHook {
      try {
        log("Shutdown hook running...")
        recorderController.stopRecording(true)
      } catch {
        case e: Exception => {
          e.printStackTrace
          log("caught above exception when trying to shut down.")
        }
      } finally {
        recordingLockFile.delete
      }
    }

    recorderController.startRecording

    val out = new FileOutputStream(recordingLockFile)
    out.write(extractProcessIdInAnArcaneFashion.getBytes("UTF-8"))
    out.close
  }

  def log(message: String) = println(s"[HEADLESS] ${message}")

  def extractProcessIdInAnArcaneFashion: String =
    ManagementFactory.getRuntimeMXBean.getName.split("@")(0)
}
