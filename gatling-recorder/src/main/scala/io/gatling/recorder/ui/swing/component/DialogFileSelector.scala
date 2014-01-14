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
package io.gatling.recorder.ui.swing.component

import scala.swing._
import scala.swing.BorderPanel.Position._

import io.gatling.recorder.ui.swing.util.UIHelper._
import io.gatling.recorder.ui.swing.frame.ConfigurationFrame

object DialogFileSelector {
	val message = """	|A Swing bug on Mac OS X prevents the Recorder from getting
						|the correct path for file with some known extensions.
						|Those files closely matches the file you selected, please select
						|the correct one :
						|""".stripMargin
}
class DialogFileSelector(configurationFrame: ConfigurationFrame, possibleFiles: List[String]) extends Dialog(configurationFrame) {

	var selectedFile: Option[String] = None

	val radioButtons = possibleFiles.map(new RadioButton(_))
	val radiosGroup = new ButtonGroup(radioButtons: _*)
	val cancelButton = Button("Cancel")(close())
	val okButton = Button("OK") { radiosGroup.selected.foreach(button => selectedFile = Some(button.text)); close() }
	val defaultBackground = background

	contents = new BorderPanel {
		val messageLabel = new TextArea(DialogFileSelector.message) { background = defaultBackground }
		val radiosPanel = new BoxPanel(Orientation.Vertical) { radioButtons.foreach(contents += _) }
		val buttonsPanel = new CenterAlignedFlowPanel {
			contents += okButton
			contents += cancelButton
		}

		layout(messageLabel) = North
		layout(radiosPanel) = Center
		layout(buttonsPanel) = South
	}

	modal = true
	setLocationRelativeTo(configurationFrame)
}
