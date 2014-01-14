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
package io.gatling.recorder.ui.swing.util

import scala.swing._

import javax.swing.JComponent

object UIHelper {

	def titledBorder(title: String) = Swing.TitledBorder(null, title)

	class LeftAlignedFlowPanel extends FlowPanel(FlowPanel.Alignment.Left)()
	class CenterAlignedFlowPanel extends FlowPanel(FlowPanel.Alignment.Center)()
	class RightAlignedFlowPanel extends FlowPanel(FlowPanel.Alignment.Right)()

	implicit def wrapComponent(component: JComponent) = Component.wrap(component)

	implicit class RichListView[T](val listView: ListView[T]) extends AnyVal {

		def add(elem: T) {
			listView.listData = listView.listData :+ elem
			listView.ensureIndexIsVisible(listView.listData.size - 1)
		}

	}

	implicit class RichTextComponent[T <: TextComponent](val textComponent: T) extends AnyVal {

		def clear {
			textComponent.text = ""
		}
	}

	implicit class RichFileChooser(val fileChooser: FileChooser) extends AnyVal {

		def selection = {
			if (fileChooser.showSaveDialog(null) != FileChooser.Result.Approve)
				None
			else
				Some(fileChooser.selectedFile.getPath)
		}
	}
}