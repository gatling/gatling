/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.recorder.ui.frame
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.Color
import java.awt.EventQueue
import java.util.Date

import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

import grizzled.slf4j.Logging
import javax.swing.BorderFactory
import javax.swing.JTextField

object ValidationHelper extends Logging {
	
	val standardBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.darkGray)
	val errorBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red)
	val disabledBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(184, 207, 229))
	
	var validationStatus = Map.empty[String,Boolean]
	
	// TODO: move table validation here
	
	def intValidator(cFrame: ConfigurationFrame, id: String) = new KeyListener {
		def keyReleased(e: KeyEvent){
			val txtField = e.getComponent.asInstanceOf[JTextField]
			try {
				txtField.getText.toInt
				if(txtField.isEnabled)
					txtField.setBorder(standardBorder)
				else
					txtField.setBorder(disabledBorder)
				updateValidationStatus(id, true, cFrame)
			} catch {
				case (e: NumberFormatException) => {
					txtField.setBorder(errorBorder)
					updateValidationStatus(id, false, cFrame)
				}
			}
		}
		
		def keyPressed(e: KeyEvent) {}
		
		def keyTyped(e: KeyEvent) {}
	}
	
	def nonEmptyValidator(cFrame: ConfigurationFrame, id: String) = new KeyListener {
		def keyReleased(e: KeyEvent) {
			val txtField = e.getComponent.asInstanceOf[JTextField]
			if(!txtField.getText.trim.isEmpty){
				txtField.setBorder(standardBorder)
				updateValidationStatus(id, true, cFrame)
			} else {
				txtField.setBorder(errorBorder)
				updateValidationStatus(id, false, cFrame)
			}
		}
		
		def keyPressed(e: KeyEvent) {}
		
		def keyTyped(e: KeyEvent) {}
	}
	
	def proxyHostValidator(cFrame: ConfigurationFrame) = new KeyListener {
		def keyReleased(e: KeyEvent) {
			val txtField = e.getComponent.asInstanceOf[JTextField]
			if(!txtField.getText.trim.isEmpty){
				cFrame.txtProxyPort.setEnabled(true)
				cFrame.txtProxySslPort.setEnabled(true)
			} else {
				cFrame.txtProxyPort.setEnabled(false)
				cFrame.txtProxyPort.setText("0")
				cFrame.txtProxyPort.getKeyListeners.foreach{
					case kl: KeyListener => 
						EventQueue.invokeLater(new Runnable() {
							def run {
								kl.keyReleased(new KeyEvent(cFrame.txtProxyPort, 0, new Date().getTime, 0, 0))
							}
						})
				}
				cFrame.txtProxySslPort.setEnabled(false)
				cFrame.txtProxySslPort.setText("0")
			}
		}
		
		def keyPressed(e: KeyEvent) {}
		
		def keyTyped(e: KeyEvent) {}
	}
	
	def updateValidationStatus(id: String, status: Boolean, cfgFrame: ConfigurationFrame){
		validationStatus += (id -> status)
		updateStartButtonStatus(cfgFrame)
	}
	
	def updateStartButtonStatus(cfgFrame: ConfigurationFrame){
		val newStatus = validationStatus.values.foldLeft(true)((b1,b2) => b1 && b2)
		cfgFrame.btnStart.setEnabled(newStatus)
	}
}