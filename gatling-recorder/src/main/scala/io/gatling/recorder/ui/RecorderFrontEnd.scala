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

import scala.swing.Dialog
import scala.swing.Swing.onEDT

import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.RecorderMode
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

	def handleMissingHarFile

	def handleHarExportSuccess

	def handleHarExportFailure

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

	def handleMissingHarFile {
		Dialog.showMessage(
			title = "Error",
			message = "You haven't selected an HAR file.",
			messageType = Dialog.Message.Error)
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
			message = "Export to HAR File unsuccessful.\nSee logs for more information",
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
}