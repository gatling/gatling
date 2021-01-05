/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.recorder.ui.swing.frame

import java.awt.Color

import scala.jdk.CollectionConverters._
import scala.swing._
import scala.swing.BorderPanel.Position._
import scala.swing.ListView.IntervalMode.Single
import scala.swing.Swing.pair2Dimension
import scala.swing.event.ListSelectionChanged

import io.gatling.commons.util.StringHelper.Eol
import io.gatling.recorder.model._
import io.gatling.recorder.ui._
import io.gatling.recorder.ui.swing.Commons.IconList
import io.gatling.recorder.ui.swing.component.TextAreaPanel
import io.gatling.recorder.ui.swing.util.UIHelper._

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.HttpHeaders

@SuppressWarnings(Array("org.wartremover.warts.LeakingSealed", "org.wartremover.warts.PublicInference"))
// LeakingSealed error is in scala-swing
private[swing] class RunningFrame(frontend: RecorderFrontEnd) extends MainFrame with StrictLogging {

  //////////////////////////////////////
  //           COMPONENTS
  //////////////////////////////////////
  /* Top panel components */
  private val tagField = new TextField(15)
  private val tagButton = Button("Add")(addTag())
  private val clearButton =
    Button("Clear") {
      clearState()
      frontend.clearRecorderState()
    }
  private val cancelButton = Button("Cancel")(frontend.stopRecording(save = false))
  private val stopButton = Button("Stop & Save")(frontend.stopRecording(save = true))

  /* Center panel components */
  private val initialSize = (472, 150)
  private val newSize = (472, 900)
  private val events = new ListView[FrontEndEvent] { selection.intervalMode = Single }
  private val requestHeaders = new TextAreaPanel("Summary", initialSize)
  private val responseHeaders = new TextAreaPanel("Summary", initialSize)
  private val requestBodies = new TextAreaPanel("Body", initialSize)
  private val responseBodies = new TextAreaPanel("Body", initialSize)
  private val infoPanels = List(requestHeaders, requestBodies, responseHeaders, responseBodies)

  /* Bottom panel components */
  private val hostsRequiringCertificates = new ListView[String] { foreground = Color.red }

  //////////////////////////////////////
  //           UI SETUP
  //////////////////////////////////////
  /* Frame setup */
  title = "Gatling Recorder - Running..."
  peer.setIconImages(IconList.asJava)

  /* Layout setup */
  val root = new BorderPanel {
    /* Top panel: Add tag, clear status, cancel recording, save simulation */
    val top = new BorderPanel {
      border = titledBorder("Controls")

      val tag = new LeftAlignedFlowPanel {
        contents += new Label("Tag:")
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
    /* Center panel: events info, request/response headers & body */
    val center = new BorderPanel {
      val elements = new BorderPanel {
        border = titledBorder("Executed Events")

        val scrollPane = new ScrollPane(events) {
          horizontalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
          verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
          preferredSize = new Dimension(400, 120)
        }

        layout(scrollPane) = Center
      }
      val requests = new BorderPanel {
        border = titledBorder("Request Information")

        layout(new SplitPane(Orientation.Horizontal, new ScrollPane(requestHeaders), new ScrollPane(requestBodies))) = Center
      }
      val responses = new BorderPanel {
        border = titledBorder("Response Information")

        layout(new SplitPane(Orientation.Horizontal, new ScrollPane(responseHeaders), new ScrollPane(responseBodies))) = Center
      }
      val info = new SplitPane(Orientation.Vertical, requests, responses)

      layout(elements) = North
      layout(info) = Center
    }
    /* Bottom panel: Secured hosts requiring certificates */
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

  //////////////////////////////////////
  //           EVENTS HANDLING
  //////////////////////////////////////
  /* Reactions */
  listenTo(events.selection)
  reactions += {
    case ListSelectionChanged(_, _, _) if events.peer.getSelectedIndex >= 0 =>
      val selectedIndex = events.peer.getSelectedIndex
      events.listData(selectedIndex) match {
        case requestInfo: RequestFrontEndEvent => showRequest(requestInfo)
        case _                                 => infoPanels.foreach(_.textArea.clear())
      }
    case _ => // Do nothing
  }

  /**
   * Add a new tag to the list of scenario elements
   */
  private def addTag(): Unit = {
    if (!tagField.text.isEmpty) {
      frontend.addTag(tagField.text)
      tagField.clear()
    }
  }

  private def headersToString(headers: HttpHeaders): String =
    headers.entries.asScala
      .map { entry =>
        s"${entry.getKey}: ${entry.getValue}"
      }
      .mkString(Eol)

  private def summary(request: HttpRequest): String = {
    import request._
    s"""$httpVersion $method $uri
       |${headersToString(headers)}""".stripMargin
  }

  private def summary(response: HttpResponse): String = {
    import response._
    s"""$status $statusText
       |${headersToString(headers)}""".stripMargin
  }

  /**
   * Display request going through the Recorder
   * @param requestInfo The outgoing request info
   */
  private def showRequest(requestInfo: RequestFrontEndEvent): Unit = {
    requestHeaders.textArea.text = summary(requestInfo.request)
    responseHeaders.textArea.text = summary(requestInfo.response)
    requestBodies.textArea.text = requestInfo.requestBody
    responseBodies.textArea.text = requestInfo.responseBody
    infoPanels.foreach(_.preferredSize = newSize)
    infoPanels.foreach(_.revalidate())
  }

  /**
   * Clear all the panels showing info about scenarios elements
   * or requests of their content
   */
  def clearState(): Unit = {
    events.clear()
    infoPanels.foreach(_.textArea.clear())
    tagField.clear()
    hostsRequiringCertificates.clear()
  }

  /**
   * Handle Recorder Events sent by the controller,
   * and display them accordingly
   * @param event the event sent by the controller
   */
  def receiveEvent(event: FrontEndEvent): Unit = {
    event match {
      case pauseInfo: PauseFrontEndEvent                                               => events.add(pauseInfo)
      case requestInfo: RequestFrontEndEvent                                           => events.add(requestInfo)
      case tagInfo: TagFrontEndEvent                                                   => events.add(tagInfo)
      case SslFrontEndEvent(uri) if !hostsRequiringCertificates.listData.contains(uri) => hostsRequiringCertificates.add(uri)
      case e                                                                           => logger.debug(s"dropping event $e")
    }
  }
}
