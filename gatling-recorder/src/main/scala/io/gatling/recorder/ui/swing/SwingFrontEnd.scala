/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import java.nio.file.Path

import scala.swing.Dialog
import scala.swing.Swing._

import io.gatling.recorder.config.{ RecorderConfiguration, RecorderMode }
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.ui.{ FrontEndEvent, RecorderFrontEnd }
import io.gatling.recorder.ui.swing.component.DialogFileSelector
import io.gatling.recorder.ui.swing.frame.{ ConfigurationFrame, RunningFrame }
import io.gatling.shared.util.PathHelper

private[ui] class SwingFrontEnd(controller: RecorderController, configuration: RecorderConfiguration) extends RecorderFrontEnd(controller) {
  private lazy val runningFrame = new RunningFrame(this)
  private lazy val configurationFrame = new ConfigurationFrame(this, configuration)

  override def selectedRecorderMode: RecorderMode = configurationFrame.selectedRecorderMode

  override def harFilePath: Path = configurationFrame.harFilePath

  override def handleMissingHarFile(harFilePath: Path): Unit =
    if (harFilePath.toString.isEmpty) {
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
        configurationFrame.updateHarFilePath(selector.selectedFile.map(file => parentPath.resolve(file)))
      }
    }

  override def handleHarExportSuccess(): Unit =
    Dialog.showMessage(
      title = "Conversion complete",
      message = "Successfully converted HAR file to a Gatling simulation",
      messageType = Dialog.Message.Info
    )

  override def handleHarExportFailure(message: String): Unit =
    Dialog.showMessage(
      title = "Error",
      message = s"""|Export to HAR File unsuccessful: $message.
                    |See logs for more information""".stripMargin,
      messageType = Dialog.Message.Error
    )

  override def handleFilterValidationFailures(failures: Seq[String]): Unit =
    Dialog.showMessage(
      title = "Error",
      message = failures.mkString("\n"),
      messageType = Dialog.Message.Error
    )

  override def askSimulationOverwrite: Boolean =
    Dialog.showConfirmation(
      title = "Warning",
      message = "You are about to overwrite an existing simulation.",
      optionType = Dialog.Options.OkCancel,
      messageType = Dialog.Message.Warning
    ) == Dialog.Result.Ok

  override def init(): Unit = {
    configurationFrame.visible = true
    runningFrame.visible = false
  }

  override def recordingStarted(): Unit = {
    runningFrame.visible = true
    configurationFrame.visible = false
  }

  override def recordingStopped(): Unit = runningFrame.clearState()

  override def receiveEvent(event: FrontEndEvent): Unit = onEDT(runningFrame.receiveEvent(event))

  private def lookupFiles(path: Path): List[String] = {
    val parent = path.getParent
    PathHelper.files(parent).collect { case p if p.path.startsWith(path) => p.filename }.toList
  }
}
