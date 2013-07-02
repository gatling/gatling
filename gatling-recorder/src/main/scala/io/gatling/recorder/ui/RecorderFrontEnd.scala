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

import javax.swing.JOptionPane

import scala.swing.Swing.onEDT

import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.RecorderMode
import io.gatling.recorder.ui.swing.frame.{ ConfigurationFrame, RunningFrame }

object RecorderFrontend {

	// Currently hardwired to the Swing frontend
	// Will select the desired frontend when more are implemented
	def newFrontend(controller: RecorderController): RecorderFrontend =
		new SwingFrontEnd(controller)
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

private class SwingFrontEnd(controller: RecorderController) extends RecorderFrontend(controller) {

	private lazy val runningFrame = new RunningFrame(this)
	private lazy val configurationFrame = new ConfigurationFrame(this)

	def selectedMode = configurationFrame.selectedMode

	def harFilePath = configurationFrame.harFilePath

	def handleMissingHarFile {
		JOptionPane.showMessageDialog(null, "You haven't selected an HAR file.", "Error", JOptionPane.ERROR_MESSAGE)
	}

	def handleHarExportSuccess {
		JOptionPane.showMessageDialog(null, "Successfully converted HAR file to a Gatling simulation", "Conversion complete", JOptionPane.INFORMATION_MESSAGE)
	}

	def handleHarExportFailure {
		JOptionPane.showMessageDialog(null, "Export to HAR File unsuccessful.\nSee logs for more information", "Error", JOptionPane.ERROR_MESSAGE)
	}

	def askSimulationOverwrite = {
		val answer = JOptionPane.showConfirmDialog(null, "You are about to overwrite an existing simulation.", "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
		answer == JOptionPane.OK_OPTION
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