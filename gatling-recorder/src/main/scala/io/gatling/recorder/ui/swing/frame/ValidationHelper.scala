/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.awt.Color

import scala.collection.mutable
import scala.swing._
import scala.swing.Swing.MatteBorder
import scala.swing.event.KeyReleased
import scala.util.Try

import io.gatling.core.util.StringHelper.RichString

object ValidationHelper {

  case class Validator(
    condition: String => Boolean,
    successCallback: Component => Unit = setStandardBorder,
    failureCallback: Component => Unit = setErrorBorder,
    alwaysValid: Boolean = false)

  def keyReleased(c: Component) = KeyReleased(c, null, 0, null)(null)

  private val standardBorder = new TextField().border
  private val errorBorder = MatteBorder(2, 2, 2, 2, Color.red)

  /* Default validators */
  private val portRange = 0 to 65536
  def isValidPort(s: String) = Try(s.toInt).toOption.exists(portRange.contains)
  def isNonEmpty(s: String) = s.trimToOption.isDefined

  /* Default callbacks */
  def setStandardBorder(c: Component) { c.border = standardBorder }
  def setErrorBorder(c: Component) { c.border = errorBorder }

  private val validators = mutable.Map.empty[TextField, Validator]
  private val status = mutable.Map.empty[TextField, Boolean]

  def registerValidator(textField: TextField, validator: Validator) {
    validators += (textField -> validator)
  }

  def updateValidationStatus(field: TextField) = validators.get(field) match {
    case Some(validator) =>
      val isValid = validator.condition(field.text)
      val callback = if (isValid) validator.successCallback else validator.failureCallback
      callback(field)
      status += (field -> (validator.alwaysValid || isValid))
    case None =>
      throw new IllegalStateException(s"No validator registered for component : $field")
  }

  def validationStatus = status.values.forall(identity)
}