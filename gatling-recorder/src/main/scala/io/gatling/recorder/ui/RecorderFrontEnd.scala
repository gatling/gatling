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
package io.gatling.recorder.ui

import io.gatling.recorder.config.{ RecorderMode, RecorderConfiguration }
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.ui.headless.HeadlessFrontend
import io.gatling.recorder.ui.swing.SwingFrontend

private[recorder] object RecorderFrontend {

  def newFrontend(controller: RecorderController)(implicit configuration: RecorderConfiguration): RecorderFrontend =
    if (configuration.core.headless) new HeadlessFrontend(controller)
    else new SwingFrontend(controller)
}
private[recorder] abstract class RecorderFrontend(controller: RecorderController) {

  /******************************/
  /**  Controller => Frontend  **/
  /******************************/

  def selectedRecorderMode: RecorderMode

  def harFilePath: String

  def handleMissingHarFile(path: String): Unit

  def handleHarExportSuccess(): Unit

  def handleHarExportFailure(message: String): Unit

  def handleFilterValidationFailures(failures: Seq[String]): Unit

  def askSimulationOverwrite: Boolean

  def init(): Unit

  def recordingStarted(): Unit

  def recordingStopped(): Unit

  def receiveEventInfo(eventInfo: EventInfo): Unit

  /******************************/
  /**  Frontend => Controller  **/
  /******************************/

  def addTag(tag: String): Unit = controller.addTag(tag)

  def startRecording(): Unit = controller.startRecording()

  def stopRecording(save: Boolean): Unit = controller.stopRecording(save)

  def clearRecorderState(): Unit = controller.clearRecorderState()
}
