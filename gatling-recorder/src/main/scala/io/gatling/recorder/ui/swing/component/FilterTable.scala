/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.awt.{ Dimension, Color, BorderLayout }
import java.awt.event.{ MouseListener, MouseEvent, MouseAdapter, ActionListener, ActionEvent }

import javax.swing.{ JTable, JScrollPane, JPopupMenu, JPanel, JMenuItem }
import javax.swing.table.DefaultTableModel

class FilterTable(header: String) extends JPanel with MouseListener {

	private val model = new DefaultTableModel
	private val table = new JTable

	model.addColumn(header)

	table.setModel(model)
	table.setRowHeight(30)
	table.getTableHeader.setReorderingAllowed(false)

	setLayout(new BorderLayout)

	val scrollPane = new JScrollPane(table)
	scrollPane.setPreferredSize(new Dimension(200, 300))
	add(scrollPane)
	scrollPane.addMouseListener(this)

	initPopupMenu

	def validateCells {
		stopCellEditing
		var toRemove: List[Int] = Nil
		for (i <- 0 until model.getRowCount if model.getValueAt(i, 0).toString.isEmpty)
			toRemove = i :: toRemove

		removeRows(toRemove)
		removeDuplicates
	}

	def removeRows(toRemove: List[Int]) {
		toRemove.sorted.reverse.foreach(model.removeRow)
	}

	def stopCellEditing {
		if (table.isEditing && table.getSelectedRow != -1)
			table.getCellEditor.stopCellEditing
	}

	def removeDuplicates {
		val toRemove = for {
			i <- 0 until model.getRowCount
			j <- i until model.getRowCount
			if i != j && getRegex(i) == getRegex(j)
		} yield j
		/* Remove the duplicated indexes and sort them in reverse order, so that we don't modify the indexes of the row we want to remove */
		toRemove.toSet.toList.sortWith(_ >= _).foreach(model.removeRow)
	}

	override def setEnabled(enabled: Boolean) {
		table.setEnabled(enabled)
		table.setBackground(if (enabled) Color.WHITE else Color.LIGHT_GRAY)
	}

	def addRow {
		stopCellEditing
		model.addRow(Array[Object](""))
	}

	def addRow(pattern: String) = model.addRow(Array[Object](pattern))

	def removeSelectedRow = removeRows(getSelectedRows.toList)

	def removeAllElements = removeRows((0 until model.getRowCount).toList)

	def getSelectedRows = table.getSelectedRows

	def getRowCount = model.getRowCount

	def getRegex(row: Int) = table.getValueAt(row, 0).asInstanceOf[String]

	def getRegexs = (for (i <- 0 until getRowCount) yield getRegex(i)).toList

	private def initPopupMenu {
		val popup = new JPopupMenu
		val menuItem = new JMenuItem("Delete")
		menuItem.addActionListener(new ActionListener {
			def actionPerformed(e: ActionEvent) {
				removeSelectedRow
			}
		})

		popup.add(menuItem)

		table.addMouseListener(new MouseAdapter() {

			override def mousePressed(e: MouseEvent) {
				maybeShowPopup(e)
			}

			override def mouseReleased(e: MouseEvent) {
				maybeShowPopup(e)
			}

			private def maybeShowPopup(e: MouseEvent) {
				if (e.isPopupTrigger)
					popup.show(e.getComponent, e.getX, e.getY)
			}
		})
	}

	override def mouseReleased(e: MouseEvent) {}

	override def mousePressed(e: MouseEvent) {}

	override def mouseExited(e: MouseEvent) {}

	override def mouseEntered(e: MouseEvent) {}

	override def mouseClicked(e: MouseEvent) {
		stopCellEditing
	}
}