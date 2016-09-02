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
package io.gatling.recorder.ui.swing

import scala.swing.Dialog
import scala.swing.Swing._

import io.gatling.commons.util.PathHelper._
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.ui.{ RecorderFrontend, EventInfo }
import io.gatling.recorder.ui.swing.component.DialogFileSelector
import io.gatling.recorder.ui.swing.frame.{ ConfigurationFrame, RunningFrame }

private[ui] class SwingFrontend(controller: RecorderController)(implicit configuration: RecorderConfiguration) extends RecorderFrontend(controller) {

  private lazy val runningFrame = new RunningFrame(this)
  private lazy val configurationFrame = new ConfigurationFrame(this)

  def selectedRecorderMode = configurationFrame.selectedRecorderMode

  def harFilePath = configurationFrame.harFilePath

  def handleMissingHarFile(harFilePath: String): Unit = {
    if (harFilePath.isEmpty) {
      Dialog.showMessage(
        title = "Error",
        message = "You haven't selected an HAR file.",
        messageType = Dialog.Message.Error
      )
    } else {
      val possibleMatches = lookupFiles(harFilePath)
      if (possibleMatches.isEmpty) {
        Dialog.showMessage(
          title = "No matches found",
          message = """|No files that could closely match the
                       |selected file's name have been found.
                       |Please check the file's path is correct.""".stripMargin,
          messageType = Dialog.Message.Warning
        )
      } else {
        val selector = new DialogFileSelector(configurationFrame, possibleMatches)
        selector.open()
        val parentPath = harFilePath.getParent
        configurationFrame.updateHarFilePath(selector.selectedFile.map(file => (parentPath / file).toString))
      }
    }
  }

  def handleHarExportSuccess(): Unit = {
    Dialog.showMessage(
      title = "Conversion complete",
      message = "Successfully converted HAR file to a Gatling simulation",
      messageType = Dialog.Message.Info
    )
  }

  def handleHarExportFailure(message: String): Unit = {
    Dialog.showMessage(
      title = "Error",
      message = s"""|Export to HAR File unsuccessful: $message.
                    |See logs for more information""".stripMargin,
      messageType = Dialog.Message.Error
    )
  }

  def handleFilterValidationFailures(failures: Seq[String]): Unit = {
    Dialog.showMessage(
      title = "Error",
      message = failures.mkString("\n"),
      messageType = Dialog.Message.Error
    )
  }

  def askSimulationOverwrite = {
    Dialog.showConfirmation(
      title = "Warning",
      message = "You are about to overwrite an existing simulation.",
      optionType = Dialog.Options.OkCancel,
      messageType = Dialog.Message.Warning
    ) == Dialog.Result.Ok
  }

  def init(): Unit = {
    configurationFrame.visible = true
    runningFrame.visible = false
  }

  def recordingStarted(): Unit = {
    runningFrame.visible = true
    configurationFrame.visible = false
  }

  def recordingStopped(): Unit = runningFrame.clearState()

  def receiveEventInfo(eventInfo: EventInfo): Unit = onEDT(runningFrame.receiveEventInfo(eventInfo))

  private def lookupFiles(path: String): List[String] = {
    val parent = path.getParent
    parent.files.filter(_.path.startsWith(path)).map(_.path.filename).toList
  }
}
