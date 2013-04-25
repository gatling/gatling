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
package com.excilys.ebi.gatling.recorder.ui.util

import java.awt.event.{ ActionEvent, ActionListener, ItemEvent, ItemListener }
import javax.swing.event.{ ListSelectionEvent, ListSelectionListener }
import com.excilys.ebi.gatling.recorder.ui.component.TextAreaPanel
import javax.swing.JTextField

class RichTextAreaPanel(area: TextAreaPanel) {
	def clear {
		area.txt.setText("")
	}
}

class RichJTextField(field: JTextField) {
	def clear {
		field.setText("")
	}
}

trait ScalaSwing {

	implicit def function2ActionListener(f: ActionEvent => Unit): ActionListener = new ActionListener {
		def actionPerformed(e: ActionEvent) {
			f(e)
		}
	}

	implicit def function2ListSelectionListener(f: ListSelectionEvent => Unit): ListSelectionListener = new ListSelectionListener {
		def valueChanged(e: ListSelectionEvent) {
			f(e)
		}
	}

	implicit def functionItemListener(f: ItemEvent => Unit): ItemListener = new ItemListener {
		def itemStateChanged(e: ItemEvent) {
			f(e)
		}
	}

	implicit def textAreaPanel2Rich(area: TextAreaPanel) = new RichTextAreaPanel(area)
	implicit def textField2Rich(field: JTextField) = new RichJTextField(field)
}