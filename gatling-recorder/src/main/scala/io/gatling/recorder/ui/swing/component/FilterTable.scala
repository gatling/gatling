/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *                 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.ui.swing.component

import java.awt.Color
import java.awt.event.{ ActionEvent, ActionListener }

import scala.swing.{ Component, Dimension, ScrollPane, Table }
import scala.swing.event.{ MouseButtonEvent, MouseEvent }
import scala.util.{ Failure, Try }

import javax.swing.{ JMenuItem, JPopupMenu }
import javax.swing.table.DefaultTableModel

class FilterTable(headerTitle: String) extends ScrollPane {

	private val table = new Table {
		override def rendererComponent(isSelected: Boolean, focused: Boolean, row: Int, column: Int): Component = {
			val c = super.rendererComponent(isSelected, focused, row, column)
			c.background = Color.lightGray
			c
		}
	}

	private val model = new DefaultTableModel

	contents = table
	model.addColumn(headerTitle)
	table.model = model
	table.rowHeight = 30
	preferredSize = new Dimension(200, 300)
	initPopupMenu

	def cleanUp() {
		stopCellEditing
		var toRemove: List[Int] = Nil
		for (i <- 0 until table.rowCount if table(i, 0).toString.isEmpty)
			toRemove = i :: toRemove

		removeRows(toRemove)
		removeDuplicates
	}

	def validate: List[String] =
		getRegexs
			.map { str => (str, Try(str.r)) }
			.collect {
				case (str, fail: Failure[_]) => s"$str is not a valid regular expression: ${fail.exception.getMessage}"
			}

	def removeRows(toRemove: Seq[Int]) {
		toRemove.sorted.reverse.foreach(model.removeRow)
	}

	def stopCellEditing() {
		if (table.peer.isEditing && table.peer.getSelectedRow != -1)
			table.peer.getCellEditor.stopCellEditing
	}

	def removeDuplicates() {
		val toRemove = for {
			i <- 0 until table.rowCount
			j <- i until table.rowCount
			if i != j && getRegex(i) == getRegex(j)
		} yield j
		/* Remove the duplicated indexes and sort them in reverse order, so that we don't modify the indexes of the row we want to remove */
		toRemove.toSet.toList.sortWith(_ >= _).foreach(model.removeRow)
	}

	def setEnabled(enabled: Boolean) {
		table.enabled = enabled
		table.background = if (enabled) Color.white else Color.lightGray
	}

	def addRow() {
		stopCellEditing
		model.addRow(Array[Object](""))
	}

	def addRow(pattern: String) { model.addRow(Array[Object](pattern)) }

	def removeSelectedRow() { removeRows(table.selection.rows.toSeq) }

	def removeAllElements() { removeRows(0 until model.getRowCount) }

	def setFocusable(focusable: Boolean) { table.focusable = focusable }

	def getRowCount = model.getRowCount

	def getRegex(row: Int) = table(row, 0).asInstanceOf[String]

	def getRegexs = (for (i <- 0 until getRowCount) yield getRegex(i)).toList

	private def initPopupMenu() {
		val popup = new JPopupMenu
		val menuItem = new JMenuItem("Delete")
		menuItem.addActionListener(new ActionListener {
			def actionPerformed(e: ActionEvent) {
				removeSelectedRow
			}
		})

		popup.add(menuItem)

		listenTo(table.mouse.clicks)

		reactions += {
			case e: MouseButtonEvent => maybeShowPopup(e)
		}

		def maybeShowPopup(e: MouseEvent) {
			if (e.peer.isPopupTrigger)
				popup.show(e.peer.getComponent, e.peer.getX, e.peer.getY)
		}
	}
}
