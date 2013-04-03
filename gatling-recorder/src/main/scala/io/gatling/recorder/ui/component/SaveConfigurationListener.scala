/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.recorder.ui.component

import scala.collection.JavaConversions.seqAsJavaList

import java.awt.event.{ ActionListener, ActionEvent }
import java.nio.charset.Charset

import io.gatling.core.util.StringHelper.trimToOption
import io.gatling.recorder.config.{ RecorderPropertiesBuilder, RecorderConfiguration }
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.ui.enumeration.FilterStrategy.FilterStrategy
import io.gatling.recorder.ui.frame.ConfigurationFrame

class SaveConfigurationListener(controller: RecorderController, configurationFrame: ConfigurationFrame) extends ActionListener {

	def actionPerformed(e: ActionEvent) {

		// validate filters
		configurationFrame.tblFilters.validateCells

		val props = new RecorderPropertiesBuilder

		// Parse local proxy port
		props.localPort(configurationFrame.txtPort.getText.toInt)

		// Parse local ssl proxy port
		props.localSslPort(configurationFrame.txtSslPort.getText.toInt)

		val host = trimToOption(configurationFrame.txtProxyHost.getText)

		if (!host.isEmpty) {
			props.proxyHost(host.get)
			// Parse outgoing proxy port
			props.proxyPort(configurationFrame.txtProxyPort.getText.toInt)

			// Parse outgoing ssl proxy port
			props.proxySslPort(configurationFrame.txtProxySslPort.getText.toInt)

			trimToOption(configurationFrame.txtProxyUsername.getText).map(props.proxyUsername)

			trimToOption(configurationFrame.txtProxyPassword.getText).map(props.proxyPassword)
		} else {
			props.proxyHost("")
			props.proxyPort(0)
			props.proxySslPort(0)
			props.proxyUsername("")
			props.proxyPassword("")
		}

		props.filterStrategy(configurationFrame.cbFilterStrategies.getSelectedItem.asInstanceOf[FilterStrategy].toString)

		// Set urls filters
		val patternsList = (for (i <- 0 until configurationFrame.tblFilters.getRowCount) yield configurationFrame.tblFilters.getPattern(i)).toList
		val (patterns,patternsType) = patternsList.map( p => (p.pattern,p.patternType)).unzip
		props.patterns(patterns)
		props.patternsType(patternsType.map(_.toString))
		// Check if a directory was entered
		props.simulationOutputFolder(configurationFrame.txtOutputFolder.getText.trim)

		props.followRedirect(configurationFrame.chkFollowRedirect.isSelected)
		props.automaticReferer(configurationFrame.chkAutomaticReferer.isSelected)

		// set selected encoding
		props.encoding(classOf[Charset].cast(configurationFrame.cbOutputEncoding.getSelectedItem).name)

		props.simulationPackage(configurationFrame.txtSimulationPackage.getText)

		props.simulationClassName(configurationFrame.txtSimulationClassName.getText.trim)

		RecorderConfiguration.reload(props.build)
		
		if (configurationFrame.chkSavePref.isSelected)
			RecorderConfiguration.saveConfig

		controller.startRecording
	}
}
