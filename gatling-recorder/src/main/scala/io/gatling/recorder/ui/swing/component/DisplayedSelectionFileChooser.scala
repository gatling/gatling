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

package io.gatling.recorder.ui.swing.component

import java.io.File
import javax.swing.filechooser.FileFilter

import scala.swing._
import scala.swing.FileChooser.SelectionMode

import io.gatling.recorder.ui.swing.keyReleased
import io.gatling.recorder.ui.swing.util.UIHelper._

private[component] class AcceptAllFileFilter extends FileFilter {
  override def accept(f: File) = true
  override def getDescription = "Accept any file filter"
}

private[swing] sealed trait ChooserType
private[swing] case object Open extends ChooserType
private[swing] case object Save extends ChooserType

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
private[swing] class DisplayedSelectionFileChooser(
    creator: Container,
    textFieldLength: Int,
    chooserType: ChooserType,
    buttonText: String = "Browse",
    selectionMode: SelectionMode.Value = SelectionMode.FilesAndDirectories,
    fileFilter: FileFilter = new AcceptAllFileFilter
) extends BoxPanel(Orientation.Horizontal) {

  val selectionDisplay = new TextField(textFieldLength)
  private val fileChooser =
    new FileChooser {
      fileSelectionMode = selectionMode
      fileFilter = fileFilter
    }
  private val openChooserButton = Button(buttonText)(fileChooserSelection().foreach(setAndPublish))

  def selection = selectionDisplay.text

  def chooserKeys = selectionDisplay.keys

  def textField = selectionDisplay

  def setPath(path: String): Unit =
    selectionDisplay.text = path

  private def fileChooserSelection() = chooserType match {
    case Open => fileChooser.openSelection()
    case Save => fileChooser.saveSelection()
  }

  private def setAndPublish(newValue: String): Unit = {
    selectionDisplay.text = newValue
    creator.publish(keyReleased(selectionDisplay))
  }

  // ------------ //
  // -- Layout -- //
  // ------------ //

  contents += selectionDisplay
  contents += openChooserButton
}
