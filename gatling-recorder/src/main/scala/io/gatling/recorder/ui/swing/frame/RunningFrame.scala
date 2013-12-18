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
package io.gatling.recorder.ui.swing.frame

import scala.collection.JavaConversions.seqAsJavaList
import scala.swing._
import scala.swing.BorderPanel.Position._
import scala.swing.ListView.IntervalMode.Single
import scala.swing.Swing.pair2Dimension
import scala.swing.event.ListSelectionChanged

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.recorder.ui._
import io.gatling.recorder.ui.swing.component.TextAreaPanel
import io.gatling.recorder.ui.swing.Commons.iconList
import io.gatling.recorder.ui.swing.util.UIHelper._

class RunningFrame(frontend: RecorderFrontend) extends MainFrame with StrictLogging {

	/************************************/
	/**           COMPONENTS           **/
	/************************************/

	/* Top panel components */
	private val tagField = new TextField(15)
	private val tagButton = Button("Add")(addTag)
	private val clearButton = Button("Clear") { clearState; frontend.clearRecorderState }
	private val cancelButton = Button("Cancel")(frontend.stopRecording(false))
	private val stopButton = Button("Stop & Save")(frontend.stopRecording(true))

	/* Center panel components */
	private val initialSize = (472, 150)
	private val newSize = (472, 900)
	private val events = new ListView[EventInfo] { selection.intervalMode = Single }
	private val requestHeaders = new TextAreaPanel("Summary", initialSize)
	private val responseHeaders = new TextAreaPanel("Summary", initialSize)
	private val requestBodies = new TextAreaPanel("Body", initialSize)
	private val responseBodies = new TextAreaPanel("Body", initialSize)
	private val infoPanels = List(requestHeaders, requestBodies, responseHeaders, responseBodies)

	/* Bottom panel components */
	private val hostsRequiringCertificates = new ListView[String]

	/**********************************/
	/**           UI SETUP           **/
	/**********************************/

	/* Frame setup */
	title = "Gatling Recorder - Running..."
	peer.setIconImages(iconList)

	/* Layout setup */
	val root = new BorderPanel {
		/* Top panel : Add tag, clear status, cancel recording, save simulation */
		val top = new BorderPanel {
			border = titledBorder("Controls")

			val tag = new LeftAlignedFlowPanel {
				contents += new Label("Tag :")
				contents += tagField
				contents += tagButton
			}

			val clear = new CenterAlignedFlowPanel { contents += clearButton }

			val cancelStop = new LeftAlignedFlowPanel {
				contents += cancelButton
				contents += stopButton
			}

			layout(tag) = West
			layout(clear) = Center
			layout(cancelStop) = East
		}
		/* Center panel : events info, request/response headers & body */
		val center = new BorderPanel {
			val elements = new BorderPanel {
				border = titledBorder("Executed Events")

				layout(new ScrollPane(events)) = Center
			}
			val requests = new BorderPanel {
				border = titledBorder("Request Information")

				layout(new SplitPane(Orientation.Horizontal, new ScrollPane(requestHeaders), new ScrollPane(requestBodies))) = Center
			}
			val responses = new BorderPanel {
				border = titledBorder("Response Information")

				layout(new SplitPane(Orientation.Horizontal, new ScrollPane(responseHeaders), new ScrollPane(responseBodies))) = Center
			}

			layout(elements) = North
			layout(requests) = West
			layout(responses) = East

		}
		/* Bottom panel : Secured hosts requiring certificates */
		val bottom = new BorderPanel {
			border = titledBorder("Secured hosts requiring accepting a certificate:")

			layout(new ScrollPane(hostsRequiringCertificates)) = Center
		}

		layout(top) = North
		layout(center) = Center
		layout(bottom) = South
	}

	val scrollPane = new ScrollPane(root)

	contents = scrollPane

	centerOnScreen()

	/*****************************************/
	/**           EVENTS HANDLING           **/
	/*****************************************/

	/* Reactions */
	listenTo(events.selection)
	reactions += {
		case ListSelectionChanged(_, _, _) if events.peer.getSelectedIndex >= 0 =>
			val selectedIndex = events.peer.getSelectedIndex
			events.listData(selectedIndex) match {
				case requestInfo: RequestInfo => showRequest(requestInfo)
				case _ => infoPanels.foreach(_.textArea.clear)
			}
		case _ => // Do nothing
	}

	/**
	 * Add a new tag to the list of scenario elements
	 */
	private def addTag {
		if (!tagField.text.isEmpty) {
			frontend.addTag(tagField.text)
			tagField.clear
		}
	}

	/**
	 * Display request going through the Recorder
	 * @param requestInfo The outgoing request info
	 */
	private def showRequest(requestInfo: RequestInfo) {
		requestHeaders.textArea.text = requestInfo.request.toString
		responseHeaders.textArea.text = requestInfo.response.toString
		requestBodies.textArea.text = requestInfo.requestBody
		responseBodies.textArea.text = requestInfo.responseBody
		infoPanels.foreach(_.preferredSize = newSize)
		infoPanels.foreach(_.revalidate)
	}

	/**
	 * Clear all the panels showing info about scenarios elements
	 * or requests of their content
	 */
	def clearState {
		events.listData = Seq.empty
		infoPanels.foreach(_.textArea.clear)
		tagField.clear
	}

	/**
	 * Handle Recorder Events sent by the controller,
	 * and display them accordingly
	 * @param eventInfo the event sent by the controller
	 */
	def receiveEventInfo(eventInfo: EventInfo) {
		eventInfo match {
			case pauseInfo: PauseInfo => events.add(pauseInfo)
			case requestInfo: RequestInfo => events.add(requestInfo)
			case tagInfo: TagInfo => events.add(tagInfo)
			case SSLInfo(uri) if !hostsRequiringCertificates.listData.contains(uri) => hostsRequiringCertificates.add(uri)
			case e => logger.debug(s"dropping event $e")
		}
	}
}
