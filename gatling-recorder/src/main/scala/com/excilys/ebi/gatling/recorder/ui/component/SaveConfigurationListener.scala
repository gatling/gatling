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
package com.excilys.ebi.gatling.recorder.ui.component;

import java.awt.event.{ ActionListener, ActionEvent }
import java.nio.charset.Charset

import com.excilys.ebi.gatling.recorder.config.Configuration.configuration
import com.excilys.ebi.gatling.recorder.config.Configuration
import com.excilys.ebi.gatling.recorder.controller.RecorderController
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterStrategy.FilterStrategy
import com.excilys.ebi.gatling.recorder.ui.frame.ConfigurationFrame

import grizzled.slf4j.Logging

class SaveConfigurationListener(configurationFrame: ConfigurationFrame) extends ActionListener with Logging {

	def actionPerformed(e: ActionEvent) {

		// validate filters
		configurationFrame.tblFilters.validateCells

		// Parse local proxy port
		configuration.port = configurationFrame.txtPort.getText.toInt

		// Parse local ssl proxy port
		configuration.sslPort = configurationFrame.txtSslPort.getText.toInt

		val uiProxyHost = configurationFrame.txtProxyHost.getText.trim
		configuration.proxy.host =
			if (uiProxyHost.isEmpty)
				None
			else
				Some(uiProxyHost)

		if (!configuration.proxy.host.isEmpty) {
			// Parse outgoing proxy port
			configuration.proxy.port = Some(configurationFrame.txtProxyPort.getText.toInt)

			// Parse outgoing ssl proxy port
			configuration.proxy.sslPort = Some(configurationFrame.txtProxySslPort.getText.toInt)
		}

		configuration.filterStrategy = configurationFrame.cbFilterStrategies.getSelectedItem.asInstanceOf[FilterStrategy]

		// Set urls filters
		configuration.patterns = (for (i <- 0 until configurationFrame.tblFilters.getRowCount) yield configurationFrame.tblFilters.getPattern(i)).toList

		// Check if a directory was entered
		configuration.outputFolder = configurationFrame.txtOutputFolder.getText.trim

		configuration.saveConfiguration = configurationFrame.chkSavePref.isSelected

		configuration.followRedirect = configurationFrame.chkFollowRedirect.isSelected

		// set selected encoding
		configuration.encoding = classOf[Charset].cast(configurationFrame.cbOutputEncoding.getSelectedItem).name

		configuration.simulationPackage = {
			val uiSimulationPackage = configurationFrame.txtSimulationPackage.getText.trim
			if (uiSimulationPackage.isEmpty)
				None
			else
				Some(uiSimulationPackage)
		}

		configuration.simulationClassName = configurationFrame.txtSimulationClassName.getText

		if (configuration.saveConfiguration)
			Configuration.saveToDisk

		debug(configuration)

		RecorderController.startRecording
	}
}
