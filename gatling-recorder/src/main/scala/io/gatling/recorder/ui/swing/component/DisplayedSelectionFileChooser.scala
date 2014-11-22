package io.gatling.recorder.ui.swing.component

import java.io.File
import javax.swing.filechooser.FileFilter

import scala.swing._
import scala.swing.FileChooser.SelectionMode

import io.gatling.recorder.ui.swing.util.UIHelper._
import io.gatling.recorder.ui.swing.keyReleased

private[component] class AcceptAllFileFilter extends FileFilter {
  override def accept(f: File) = true
  override def getDescription = "Accept any file filter"
}

sealed trait ChooserType
case object Open extends ChooserType
case object Save extends ChooserType

class DisplayedSelectionFileChooser(
  creator: Container,
  textFieldLength: Int,
  chooserType: ChooserType,
  buttonText: String = "Browse",
  selectionMode: SelectionMode.Value = SelectionMode.FilesAndDirectories,
  fileFilter: FileFilter = new AcceptAllFileFilter)
    extends BoxPanel(Orientation.Horizontal) {

  val selectionDisplay = new TextField(textFieldLength)
  private val fileChooser = new FileChooser { fileSelectionMode = selectionMode; fileFilter = fileFilter }
  private val openChooserButton = Button("Browse")(fileChooserSelection().foreach(setAndPublish))

  def selection = selectionDisplay.text

  def chooserKeys = selectionDisplay.keys

  def textField = selectionDisplay

  def setPath(path: String): Unit =
    selectionDisplay.text = path

  private def fileChooserSelection() = chooserType match {
    case Open => fileChooser.openSelection()
    case Save => fileChooser.saveSelection()
  }

  private def setAndPublish(newValue: String) = {
    selectionDisplay.text = newValue
    creator.publish(keyReleased(selectionDisplay))
  }

  // ------------ //
  // -- Layout -- //
  // ------------ //

  contents += selectionDisplay
  contents += openChooserButton
}
