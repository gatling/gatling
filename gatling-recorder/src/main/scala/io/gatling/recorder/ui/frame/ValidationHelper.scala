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
package io.gatling.recorder.ui.frame

import java.awt.Color
import java.awt.event.{ KeyAdapter, KeyEvent }

import scala.collection.mutable

import io.gatling.core.util.StringHelper.RichString
import io.gatling.recorder.ui.util.UIHelper.useUIThread

import javax.swing.{ JTextField, BorderFactory }

object ValidationHelper {

	private val standardBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.darkGray)
	private val errorBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red)
	private val disabledBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(184, 207, 229))

	private val validationStatus = mutable.Map.empty[String, Boolean]

	// TODO: move table validation here

	def intValidator(cFrame: ConfigurationFrame, id: String) = new KeyAdapter {
		override def keyReleased(e: KeyEvent) {
			val txtField = e.getComponent.asInstanceOf[JTextField]
			try {
				txtField.getText.toInt
				if (txtField.isEnabled)
					txtField.setBorder(standardBorder)
				else
					txtField.setBorder(disabledBorder)
				updateValidationStatus(id, true, cFrame)
			} catch {
				case e: NumberFormatException =>
					txtField.setBorder(errorBorder)
					updateValidationStatus(id, false, cFrame)
			}
		}
	}

	def nonEmptyValidator(cFrame: ConfigurationFrame, id: String) = new KeyAdapter {
		override def keyReleased(e: KeyEvent) {
			val txtField = e.getComponent.asInstanceOf[JTextField]

			txtField.getText.trimToOption.map { _ =>
				txtField.setBorder(standardBorder)
				updateValidationStatus(id, true, cFrame)
			}.getOrElse {
				txtField.setBorder(errorBorder)
				updateValidationStatus(id, false, cFrame)
			}
		}
	}

	def proxyHostValidator(cFrame: ConfigurationFrame) = new KeyAdapter {
		override def keyReleased(e: KeyEvent) {
			val txtField = e.getComponent.asInstanceOf[JTextField]

			txtField.getText.trimToOption.map { _ =>
				cFrame.txtProxyPort.setEnabled(true)
				cFrame.txtProxySslPort.setEnabled(true)
				cFrame.txtProxyUsername.setEnabled(true)
				cFrame.txtProxyPassword.setEnabled(true)
			}.getOrElse {
				cFrame.txtProxyPort.setEnabled(false)
				cFrame.txtProxyPort.setText("0")
				cFrame.txtProxyPort.getKeyListeners.foreach {
					kl =>
						useUIThread {
							kl.keyReleased(new KeyEvent(cFrame.txtProxyPort, 0, System.currentTimeMillis, 0, 0, '0'))
						}
				}
				cFrame.txtProxySslPort.setEnabled(false)
				cFrame.txtProxySslPort.setText("0")
				cFrame.txtProxyUsername.setEnabled(false)
				cFrame.txtProxyUsername.setText(null)
				cFrame.txtProxyPassword.setEnabled(false)
				cFrame.txtProxyPassword.setText(null)
			}
		}
	}

	private def updateValidationStatus(id: String, status: Boolean, cfgFrame: ConfigurationFrame) {
		validationStatus += (id -> status)
		updateStartButtonStatus(cfgFrame)
	}

	private def updateStartButtonStatus(cfgFrame: ConfigurationFrame) {
		val newStatus = validationStatus.values.foldLeft(true)((b1, b2) => b1 && b2)
		cfgFrame.btnStart.setEnabled(newStatus)
	}
}