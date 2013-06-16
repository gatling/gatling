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
package io.gatling.recorder.ui.component

import java.awt.FileDialog

import scala.swing.{ FileChooser, Frame }
import scala.util.Properties.osName

object Chooser {
	private val isMacOSX = osName.startsWith("Mac")

	if (isMacOSX) {
		// on mac, use native dialog because JFileChooser is buggy
		System.setProperty("apple.awt.fileDialogForDirectories", "true")
	}

	def apply(mode: FileChooser.SelectionMode.Value, frame: Frame) = {
		if (isMacOSX)
			new ChooserUsingFileDialog(frame)
		else
			new ChooserUsingFileChooser(mode)
	}
}

sealed trait Chooser {
	def selection: Option[String]
}

class ChooserUsingFileDialog(frame: Frame) extends Chooser {
	val fileDialog = new FileDialog(frame.peer)

	def selection = {
		fileDialog.setVisible(true)
		Option(fileDialog.getDirectory).map(_ + fileDialog.getFile)
	}
}

class ChooserUsingFileChooser(mode: FileChooser.SelectionMode.Value) extends Chooser {
	val fileChooser = new FileChooser { fileSelectionMode = mode }

	def selection = {
		if (fileChooser.showSaveDialog(null) != FileChooser.Result.Approve)
			None
		else
			Some(fileChooser.selectedFile.getPath)
	}
}