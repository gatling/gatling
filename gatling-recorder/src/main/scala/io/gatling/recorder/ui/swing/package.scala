package io.gatling.recorder.ui

import scala.swing.Component
import scala.swing.event.KeyReleased

package object swing {

  def keyReleased(c: Component) = KeyReleased(c, null, 0, null)(null)
}
