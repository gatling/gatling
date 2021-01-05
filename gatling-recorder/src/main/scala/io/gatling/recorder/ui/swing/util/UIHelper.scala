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

package io.gatling.recorder.ui.swing.util

import javax.swing.JComponent
import javax.swing.border.TitledBorder

import scala.swing._

private[swing] object UIHelper {

  def titledBorder(title: String): TitledBorder = Swing.TitledBorder(null, title)

  class LeftAlignedFlowPanel extends FlowPanel(FlowPanel.Alignment.Left)()
  class CenterAlignedFlowPanel extends FlowPanel(FlowPanel.Alignment.Center)()
  class RightAlignedFlowPanel extends FlowPanel(FlowPanel.Alignment.Right)()

  implicit def wrapComponent(component: JComponent): Component = Component.wrap(component)

  implicit class RichListView[T](val listView: ListView[T]) extends AnyVal {

    def add(elem: T): Unit = {
      listView.listData = listView.listData :+ elem
      listView.ensureIndexIsVisible(listView.listData.size - 1)
    }

    def clear(): Unit = {
      listView.listData = Seq.empty
    }
  }

  implicit class RichTextComponent[T <: TextComponent](val textComponent: T) extends AnyVal {

    def clear(): Unit =
      textComponent.text = ""
  }

  implicit class RichFileChooser(val fileChooser: FileChooser) extends AnyVal {

    def openSelection(): Option[String] = selection(fileChooser.showOpenDialog(null))
    def saveSelection(): Option[String] = selection(fileChooser.showSaveDialog(null))

    private def selection(result: FileChooser.Result.Value) =
      if (result != FileChooser.Result.Approve)
        None
      else
        Some(fileChooser.selectedFile.getPath)
  }

}
