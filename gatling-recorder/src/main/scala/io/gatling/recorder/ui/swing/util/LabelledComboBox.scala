package io.gatling.recorder.ui.swing.util

import io.gatling.recorder.util.Labelled

import scala.swing.ComboBox
import scala.swing.ListView.Renderer

class LabelledComboBox[T <: Labelled](elements: List[T]) extends ComboBox[T](elements) {
  selection.index = 0
  renderer = Renderer(_.label)
}
