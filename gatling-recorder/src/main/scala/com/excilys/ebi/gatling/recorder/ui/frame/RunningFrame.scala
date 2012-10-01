/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.recorder.ui.frame

import java.awt.{ FlowLayout, Dimension, BorderLayout }
import java.awt.event.{ ActionListener, ActionEvent }

import scala.collection.JavaConversions.seqAsJavaList

import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.recorder.controller.RecorderController
import com.excilys.ebi.gatling.recorder.ui.Commons.iconList
import com.excilys.ebi.gatling.recorder.ui.component.TextAreaPanel
import com.excilys.ebi.gatling.recorder.ui.info.{ TagInfo, SSLInfo, RequestInfo, PauseInfo, EventInfo }

import grizzled.slf4j.Logging
import javax.swing.{ JTextField, JSplitPane, JScrollPane, JPanel, JList, JLabel, JFrame, JButton, DefaultListModel, BorderFactory }
import javax.swing.event.{ ListSelectionListener, ListSelectionEvent }

class RunningFrame(controller: RecorderController) extends JFrame with Logging {

	private val btnTag = new JButton("Add")
	private val btnClear = new JButton("Clear")
	private val btnCancel = new JButton("Cancel")
	private val btnStop = new JButton("Stop & Save")

	private val txtTag = new JTextField(15)

	private val eventsInfo = new DefaultListModel
	private val hostsCertificate = new DefaultListModel

	private val eventsInfoJList = new JList(eventsInfo)
	private val requiredHostsCertificate = new JList(hostsCertificate)

	private val requestHeadersInfo = new TextAreaPanel("Summary")
	private val responseHeadersInfo = new TextAreaPanel("Summary")
	private val requestBodyInfo = new TextAreaPanel("Body")
	private val responseBodyInfo = new TextAreaPanel("Body")

	/* Initialization of the frame */
	setTitle("Gatling Recorder - Running...")
	setLayout(new BorderLayout)
	setMinimumSize(new Dimension(1024, 768))
	setLocationRelativeTo(null)
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

	setIconImages(iconList)

	/* Top Panel */
	val topPanel = new JPanel(new BorderLayout)
	topPanel.setBorder(BorderFactory.createTitledBorder("Controls"))

	val tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
	tagPanel.add(new JLabel("Tag :"))
	tagPanel.add(txtTag)
	tagPanel.add(btnTag)

	val clearPanel = new JPanel(new FlowLayout(FlowLayout.CENTER))
	clearPanel.add(btnClear)

	val stopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
	stopPanel.add(btnCancel)
	stopPanel.add(btnStop)

	topPanel.add(tagPanel, BorderLayout.WEST)
	topPanel.add(clearPanel, BorderLayout.CENTER)
	topPanel.add(stopPanel, BorderLayout.EAST)

	/* Center Panel */
	val centerPanel = new JPanel(new BorderLayout)

	val elementsPanel = new JPanel(new BorderLayout)
	elementsPanel.setBorder(BorderFactory.createTitledBorder("Executed Events"))
	elementsPanel.add(new JScrollPane(eventsInfoJList), BorderLayout.CENTER)

	val defaultDimension = new Dimension(472, 150)

	requestHeadersInfo.setPreferredSize(defaultDimension)
	responseHeadersInfo.setPreferredSize(defaultDimension)
	requestBodyInfo.setPreferredSize(defaultDimension)
	responseBodyInfo.setPreferredSize(defaultDimension)

	val requestInformationPanel = new JPanel(new BorderLayout)
	requestInformationPanel.setBorder(BorderFactory.createTitledBorder("Request Information"))
	requestInformationPanel.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(requestHeadersInfo), new JScrollPane(requestBodyInfo)), BorderLayout.CENTER)

	val responseInformationPanel = new JPanel(new BorderLayout)
	responseInformationPanel.setBorder(BorderFactory.createTitledBorder("Response Information"))
	responseInformationPanel.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(responseHeadersInfo), new JScrollPane(responseBodyInfo)), BorderLayout.CENTER)

	centerPanel.add(elementsPanel, BorderLayout.NORTH)
	centerPanel.add(requestInformationPanel, BorderLayout.WEST)
	centerPanel.add(responseInformationPanel, BorderLayout.EAST)

	/* Bottom Panel */

	val bottomPanel = new JPanel(new BorderLayout)
	bottomPanel.setBorder(BorderFactory.createTitledBorder("Secured hosts requiring accepting a certificate:"))

	val panelHostsCertificate = new JScrollPane(requiredHostsCertificate)

	bottomPanel.add(panelHostsCertificate, BorderLayout.CENTER)

	/* Layout */
	add(topPanel, BorderLayout.NORTH)
	add(centerPanel, BorderLayout.CENTER)
	add(bottomPanel, BorderLayout.SOUTH)

	setListeners

	private def setListeners {
		/* Listeners */
		btnTag.addActionListener(new ActionListener {
			def actionPerformed(e: ActionEvent) {
				if (!(txtTag.getText == EMPTY)) {
					val tag = new TagInfo(txtTag.getText)
					eventsInfo.addElement(tag)
					controller.addTag(txtTag.getText)
					eventsInfoJList.ensureIndexIsVisible(eventsInfo.getSize() - 1)
					txtTag.setText(EMPTY)
				}
			}
		});

		eventsInfoJList.addListSelectionListener(new ListSelectionListener {
			override def valueChanged(e: ListSelectionEvent) {
				if (eventsInfoJList.getSelectedIndex() >= 0) {
					val obj = eventsInfo.get(eventsInfoJList.getSelectedIndex());
					if (obj.isInstanceOf[RequestInfo]) {
						val requestInfo = obj.asInstanceOf[RequestInfo]
						requestHeadersInfo.txt.setText(requestInfo.request.toString)
						responseHeadersInfo.txt.setText(requestInfo.response.toString)
						requestBodyInfo.txt.setText(requestInfo.requestBody)
						responseBodyInfo.txt.setText(requestInfo.responseBody)
						val newDimension = new Dimension(472, 900)
						requestHeadersInfo.setPreferredSize(newDimension)
						responseHeadersInfo.setPreferredSize(newDimension)
						requestBodyInfo.setPreferredSize(newDimension)
						responseBodyInfo.setPreferredSize(newDimension)
						requestHeadersInfo.revalidate
						requestBodyInfo.revalidate
						responseHeadersInfo.revalidate
						responseBodyInfo.revalidate
					} else {
						requestHeadersInfo.txt.setText(EMPTY)
						responseHeadersInfo.txt.setText(EMPTY)
						requestBodyInfo.txt.setText(EMPTY)
						responseBodyInfo.txt.setText(EMPTY)
					}
				}
			}
		})

		btnClear.addActionListener(new ActionListener {
			def actionPerformed(e: ActionEvent) {
				controller.clearRecorderState
			}
		})

		btnCancel.addActionListener(new ActionListener {
			def actionPerformed(e: ActionEvent) {
				controller.clearRecorderState
				controller.stopRecording
			}
		})

		btnStop.addActionListener(new ActionListener {
			def actionPerformed(e: ActionEvent) {
				controller.stopRecording
			}
		})
	}

	def clearState {
		eventsInfo.removeAllElements
		requestHeadersInfo.txt.setText(EMPTY)
		requestBodyInfo.txt.setText(EMPTY)
		responseHeadersInfo.txt.setText(EMPTY)
		responseBodyInfo.txt.setText(EMPTY)
		eventsInfo.clear
	}

	def receiveEventInfo(eventInfo: EventInfo) {
		eventInfo match {
			case pauseInfo: PauseInfo =>
				eventsInfo.addElement(pauseInfo)
				eventsInfoJList.ensureIndexIsVisible(eventsInfo.getSize - 1)
			case requestInfo: RequestInfo =>
				eventsInfo.addElement(requestInfo)
				eventsInfoJList.ensureIndexIsVisible(eventsInfo.getSize - 1)
			case SSLInfo(uri) if (!hostsCertificate.contains(uri)) => hostsCertificate.addElement(uri)
			case e => debug("dropping event " + e)
		}
	}
}
