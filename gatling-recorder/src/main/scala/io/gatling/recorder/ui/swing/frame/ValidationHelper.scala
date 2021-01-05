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

import scala.collection.mutable
import scala.swing._
import scala.swing.Swing.MatteBorder
import scala.util.Try

import io.gatling.commons.util.StringHelper.RichString

private[swing] object ValidationHelper {

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  final case class Validator(
      condition: String => Boolean,
      successCallback: Component => Unit = setStandardBorder,
      failureCallback: Component => Unit = setErrorBorder,
      alwaysValid: Boolean = false
  )

  // Those are lazy vals to avoid unneccessary component creation when they're not needed (e.g. tests)
  private lazy val standardBorder = new TextField().border
  private lazy val errorBorder = MatteBorder(2, 2, 2, 2, Color.red)

  /* Default validators */
  private val portRange = 0 to 65536
  def isValidPort(s: String) = Try(s.toInt).toOption.exists(portRange.contains)
  def isNonEmpty(s: String) = s.trimToOption.isDefined

  private val validPackageNameRegex = """^[a-z_\$][\w\$]*(?:\.[a-z_\$][\w\$]*)*$"""
  def isValidPackageName(s: String) =
    s.isEmpty ||
      s.matches(validPackageNameRegex)

  def isValidSimpleClassName(s: String) =
    isNonEmpty(s) &&
      !s.contains('_') &&
      Character.isJavaIdentifierStart(s.charAt(0)) &&
      !s.substring(1, s.length).exists(!Character.isJavaIdentifierPart(_))

  /* Default callbacks */
  def setStandardBorder(c: Component): Unit = { c.border = standardBorder }
  def setErrorBorder(c: Component): Unit = { c.border = errorBorder }

  private val validators = mutable.Map.empty[TextField, Validator]
  private val status = mutable.Map.empty[TextField, Boolean]
  private val ignoredStatus = mutable.Map.empty[TextField, Boolean]

  def registerValidator(textField: TextField, validator: Validator): Unit = {
    validators += (textField -> validator)
  }

  def updateValidationStatus(field: TextField) = validators.get(field) match {
    case Some(validator) =>
      val isValid = validator.condition(field.text)
      val callback = if (isValid) validator.successCallback else validator.failureCallback
      callback(field)
      status += (field -> (validator.alwaysValid || isValid))
    case _ =>
      throw new IllegalStateException(s"No validator registered for component : $field")
  }

  def ignoreValidation(field: TextField, value: Boolean) = validators.get(field) match {
    case Some(_) =>
      if (value) {
        ignoredStatus += (field -> true)
      } else {
        ignoredStatus -= field
      }

    case _ =>
      throw new IllegalStateException(s"No validator registered for component : $field")
  }

  def allValid: Boolean = {
    validators.keys.map(updateValidationStatus)
    validationStatus
  }

  def validationStatus: Boolean =
    status.forall { case (field, s) => s || ignoredStatus.getOrElse(field, false) }
}
