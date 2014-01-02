/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.recorder.ui

import scala.reflect.io.Path.string2path
import scala.swing.Dialog
import scala.swing.Swing.onEDT

import io.gatling.recorder.RecorderMode
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.ui.swing.component.DialogFileSelector
import io.gatling.recorder.ui.swing.frame.{ ConfigurationFrame, RunningFrame }

object RecorderFrontend {

	// Currently hardwired to the Swing frontend
	// Will select the desired frontend when more are implemented
	def newFrontend(controller: RecorderController): RecorderFrontend =
		new SwingFrontend(controller)
}
sealed abstract class RecorderFrontend(controller: RecorderController) {

	/******************************/
	/**  Controller => Frontend  **/
	/******************************/

	def selectedMode: RecorderMode

	def harFilePath: String

	def handleMissingHarFile(path: String)

	def handleHarExportSuccess

	def handleHarExportFailure

	def handleFilterValidationFailures(failures: Seq[String])

	def askSimulationOverwrite: Boolean

	def init

	def recordingStarted

	def recordingStopped

	def receiveEventInfo(eventInfo: EventInfo)

	/******************************/
	/**  Frontend => Controller  **/
	/******************************/

	def addTag(tag: String) {
		controller.addTag(tag)
	}

	def startRecording {
		controller.startRecording
	}

	def stopRecording(save: Boolean) {
		controller.stopRecording(save)
	}

	def clearRecorderState {
		controller.clearRecorderState
	}
}

private class SwingFrontend(controller: RecorderController) extends RecorderFrontend(controller) {

	private lazy val runningFrame = new RunningFrame(this)
	private lazy val configurationFrame = new ConfigurationFrame(this)

	def selectedMode = configurationFrame.selectedMode

	def harFilePath = configurationFrame.harFilePath

	def handleMissingHarFile(harFilePath: String) {
		if (harFilePath.isEmpty) {
			Dialog.showMessage(
				title = "Error",
				message = "You haven't selected an HAR file.",
				messageType = Dialog.Message.Error)
		} else {
			val possibleMatches = lookupFiles(harFilePath)
			if (possibleMatches.isEmpty) {
				Dialog.showMessage(
					title = "No matches found",
					message = """	|No files that could closely match the
									|selected file's name have been found.
									|Please check the file's path is correct.""".stripMargin,
					messageType = Dialog.Message.Warning)
			} else {
				val selector = new DialogFileSelector(configurationFrame, possibleMatches)
				selector.open()
				val parentPath = harFilePath.parent.path
				configurationFrame.updateHarFilePath(selector.selectedFile.map(file => (parentPath / file).toString))
			}
		}
	}

	def handleHarExportSuccess {
		Dialog.showMessage(
			title = "Conversion complete",
			message = "Successfully converted HAR file to a Gatling simulation",
			messageType = Dialog.Message.Info)
	}

	def handleHarExportFailure {
		Dialog.showMessage(
			title = "Error",
			message = """	|Export to HAR File unsuccessful.
							|See logs for more information""".stripMargin,
			messageType = Dialog.Message.Error)
	}

	def handleFilterValidationFailures(failures: Seq[String]) {
		Dialog.showMessage(
			title = "Error",
			message = failures.mkString("\n"),
			messageType = Dialog.Message.Error)
	}

	def askSimulationOverwrite = {
		Dialog.showConfirmation(
			title = "Warning",
			message = "You are about to overwrite an existing simulation.",
			optionType = Dialog.Options.OkCancel,
			messageType = Dialog.Message.Warning) == Dialog.Result.Ok
	}

	def init {
		configurationFrame.visible = true
		runningFrame.visible = false
	}

	def recordingStarted {
		runningFrame.visible = true
		configurationFrame.visible = false
	}

	def recordingStopped {
		runningFrame.clearState
	}

	def receiveEventInfo(eventInfo: EventInfo) {
		onEDT(runningFrame.receiveEventInfo(eventInfo))
	}

	private def lookupFiles(path: String) = {
		val parent = path.parent
		parent.files.filter(_.path.startsWith(path)).map(_.name).toList
	}
}