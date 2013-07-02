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
package io.gatling.recorder.ui.swing.frame

import java.nio.charset.Charset

import scala.collection.JavaConversions.{ collectionAsScalaIterable, seqAsJavaList }
import scala.swing._
import scala.swing.BorderPanel.Position._
import scala.swing.ListView.Renderer
import scala.swing.Swing.pair2Dimension
import scala.swing.event.{ KeyReleased, SelectionChanged }
import scala.util.Try

import io.gatling.core.util.StringHelper.RichString
import io.gatling.recorder.{ Har, Proxy, RecorderMode }
import io.gatling.recorder.config.{ RecorderConfiguration, RecorderPropertiesBuilder }
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.enumeration.FilterStrategy
import io.gatling.recorder.ui.RecorderFrontend
import io.gatling.recorder.ui.swing.Commons.{ iconList, logoSmall }
import io.gatling.recorder.ui.swing.component.{ Chooser, FilterTable }
import io.gatling.recorder.ui.swing.util.UIHelper._
import io.gatling.recorder.ui.swing.frame.ValidationHelper._

class ConfigurationFrame(frontend: RecorderFrontend) extends MainFrame {

	/************************************/
	/**           COMPONENTS           **/
	/************************************/

	/* Top panel components */
	private val modeSelector = new ComboBox[RecorderMode](Seq(Proxy, Har)) {
		selection.index = 0
		renderer = Renderer(_.name)
	}

	/* Network panel components */
	private val localProxyHttpPort = new TextField(4)
	private val localProxyHttpsPort = new TextField(4)
	private val outgoingProxyHost = new TextField(12)
	private val outgoingProxyHttpPort = new TextField(4) { enabled = false }
	private val outgoingProxyHttpsPort = new TextField(4) { enabled = false }
	private val outgoingProxyUsername = new TextField(10) { enabled = false }
	private val outgoingProxyPassword = new TextField(10) { enabled = false }

	/* Har Panel components */
	private val harPath = new TextField(66)
	private val harFileChooser = Chooser(FileChooser.SelectionMode.FilesOnly, this)
	private val harFileBrowserButton = Button("Browse")(harFileChooser.selection.foreach(harPath.text = _))

	/* Simulation panel components */
	private val simulationPackage = new TextField(30)
	private val simulationClassName = new TextField(30)
	private val followRedirects = new CheckBox("Follow Redirects?")
	private val automaticReferers = new CheckBox("Automatic Referers?")

	/* Output panel components */
	private val outputEncoding = new ComboBox[Charset](Charset.availableCharsets.values.toSeq)
	private val outputFolderPath = new TextField(66)
	private val outputFolderChooser = Chooser(FileChooser.SelectionMode.DirectoriesOnly, this)
	private val outputFolderBrowserButton = Button("Browse")(outputFolderChooser.selection.foreach(outputFolderPath.text = _))

	/* Filters panel components */
	private val filtersTable = new FilterTable
	private val filterStrategies = new ComboBox[FilterStrategy.Value](FilterStrategy.values.toSeq)
	private val addFilter = Button("+")(filtersTable.addRow)
	private val removeFilter = Button("-")(filtersTable.removeSelectedRow)
	private val clearFilters = Button("Clear")(filtersTable.removeAllElements)

	/* Bottom panel components */
	private val savePreferences = new CheckBox("Save preferences") { horizontalTextPosition = Alignment.Left }
	private val start = Button("Start !")(reloadConfigurationAndStart)

	registerValidators
	populateItemsFromConfiguration

	/**********************************/
	/**           UI SETUP           **/
	/**********************************/

	/* Frame setup */
	title = "Gatling Recorder - Configuration"
	minimumSize = (1024, 768)
	resizable = true
	centerOnScreen()
	peer.setIconImages(iconList)

	/* Layout setup */
	val root = new BorderPanel {
		/* Top panel : Gatling logo & Recorder mode */
		val top = new BorderPanel {
			val logo = new CenterAlignedFlowPanel { contents += new Label { icon = logoSmall } }
			val modeSelection = new BorderPanel {
				border = titledBorder("Recorder mode")
				layout(modeSelector) = Center
			}

			layout(logo) = West
			layout(modeSelection) = East
		}
		/* Center panel : network config or har import, simulation config, output config & filters */
		val center = new BoxPanel(Orientation.Vertical) {
			val network = new BorderPanel {
				border = titledBorder("Network")

				val localProxy = new LeftAlignedFlowPanel {
					contents += new Label("Listening port* : ")
					contents += new Label("    localhost")
					contents += new Label("HTTP")
					contents += localProxyHttpPort
					contents += new Label("HTTPS")
					contents += localProxyHttpsPort

				}
				val outgoingProxy = new LeftAlignedFlowPanel {
					contents += new Label("Outgoing proxy : ")
					contents += new Label("host:")
					contents += outgoingProxyHost
					contents += new Label("HTTP")
					contents += outgoingProxyHttpPort
					contents += new Label("HTTPS")
					contents += outgoingProxyHttpsPort
					contents += new Label("Username")
					contents += outgoingProxyUsername
					contents += new Label("Password")
					contents += outgoingProxyPassword
				}

				layout(localProxy) = North
				layout(outgoingProxy) = South
			}
			val har = new BorderPanel {
				border = titledBorder("Http Archive (HAR) Import")
				visible = false

				val fileSelection = new LeftAlignedFlowPanel {
					contents += new Label("HAR File : ")
					contents += harPath
					contents += harFileBrowserButton
				}

				layout(fileSelection) = Center
			}
			val simulationConfig = new BorderPanel {
				border = titledBorder("Simulation Information")

				val config = new BorderPanel {
					val packageName = new LeftAlignedFlowPanel {
						contents += new Label("Package: ")
						contents += simulationPackage
					}
					val className = new LeftAlignedFlowPanel {
						contents += new Label("Class Name*: ")
						contents += simulationClassName
					}

					layout(packageName) = West
					layout(className) = East
				}

				layout(config) = North
				layout(followRedirects) = West
				layout(automaticReferers) = East
			}
			val outputConfig = new BorderPanel {
				border = titledBorder("Output")

				val folderSelection = new LeftAlignedFlowPanel {
					contents += new Label("Output folder* : ")
					contents += outputFolderPath
					contents += outputFolderBrowserButton
				}
				val encoding = new LeftAlignedFlowPanel {
					contents += new Label("Encoding: ")
					contents += outputEncoding
				}

				layout(folderSelection) = North
				layout(encoding) = Center
			}
			val filters = new BorderPanel {
				border = titledBorder("Filters")

				val actions = new CenterAlignedFlowPanel {
					contents += new Label("Strategy")
					contents += filterStrategies
					contents += addFilter
					contents += removeFilter
					contents += clearFilters
				}

				layout(filtersTable) = Center
				layout(actions) = South
			}

			contents += network
			contents += har
			contents += simulationConfig
			contents += outputConfig
			contents += filters
		}
		/* Bottom panel : Save preferences & start recording/ export HAR */
		val bottom = new RightAlignedFlowPanel {
			contents += savePreferences
			contents += start
		}

		layout(top) = North
		layout(center) = Center
		layout(bottom) = South
	}

	contents = root

	/*****************************************/
	/**           EVENTS HANDLING           **/
	/*****************************************/

	/* Reactions I : handling filters table edition and switching between Proxy and HAR mode */
	listenTo(filterStrategies.selection, modeSelector.selection)
	// Backticks are needed to match the components, see section 8.1.5 of Scala spec.
	reactions += {
		case SelectionChanged(`modeSelector`) =>
			modeSelector.selection.item match {
				case Proxy =>
					root.center.network.visible = true
					root.center.har.visible = false
				case Har =>
					root.center.network.visible = false
					root.center.har.visible = true
			}
		case SelectionChanged(`filterStrategies`) =>
			val isNotNoneStrategy = filterStrategies.selection.item != FilterStrategy.NONE
			toggleFiltersEdition(isNotNoneStrategy)
	}

	private def toggleFiltersEdition(enabled: Boolean) {
		filtersTable.setEnabled(enabled)
		filtersTable.setFocusable(enabled)
	}

	/* Reactions II : fields validation */
	listenTo(localProxyHttpPort.keys, localProxyHttpsPort.keys)
	listenTo(outgoingProxyHost.keys, outgoingProxyHttpPort.keys, outgoingProxyHttpsPort.keys)
	listenTo(outputFolderPath.keys, simulationClassName.keys)

	private def registerValidators {
		ValidationHelper.registerValidator(localProxyHttpPort, Validator(isValidPort))
		ValidationHelper.registerValidator(localProxyHttpsPort, Validator(isValidPort))
		ValidationHelper.registerValidator(outgoingProxyHost, Validator(isNonEmpty, enableConfig, disableConfig, true))
		ValidationHelper.registerValidator(outgoingProxyHttpPort, Validator(isValidPort))
		ValidationHelper.registerValidator(outgoingProxyHttpsPort, Validator(isValidPort))
		ValidationHelper.registerValidator(outputFolderPath, Validator(isNonEmpty))
		ValidationHelper.registerValidator(simulationClassName, Validator(isNonEmpty))
	}

	private def enableConfig(c: Component) {
		outgoingProxyHttpPort.enabled = true
		outgoingProxyHttpsPort.enabled = true
		outgoingProxyUsername.enabled = true
		outgoingProxyPassword.enabled = true
	}

	private def disableConfig(c: Component) {
		outgoingProxyHttpPort.enabled = false
		outgoingProxyHttpsPort.enabled = false
		outgoingProxyUsername.enabled = false
		outgoingProxyPassword.enabled = false
		outgoingProxyHttpPort.text = "0"
		outgoingProxyHttpsPort.text = "0"
		outgoingProxyUsername.text = null
		outgoingProxyPassword.text = null
		publish(keyReleased(outgoingProxyHttpPort))
		publish(keyReleased(outgoingProxyHttpsPort))
	}

	reactions += {
		case KeyReleased(field, _, _, _) =>
			updateValidationStatus(field.asInstanceOf[TextField])
			start.enabled = ValidationHelper.validationStatus
	}

	def selectedMode = modeSelector.selection.item

	def harFilePath = harPath.text

	/****************************************/
	/**           CONFIGURATION            **/
	/****************************************/

	/**
	 * Configure fields, checkboxes, filters... based on the current Recorder configuration
	 */
	private def populateItemsFromConfiguration {
		localProxyHttpPort.text = configuration.proxy.port.toString
		localProxyHttpsPort.text = configuration.proxy.sslPort.toString

		configuration.proxy.outgoing.host.map { proxyHost =>
			outgoingProxyHost.text = proxyHost
			outgoingProxyHttpPort.text = configuration.proxy.outgoing.port.map(_.toString).orNull
			outgoingProxyHttpsPort.text = configuration.proxy.outgoing.sslPort.map(_.toString).orNull
			outgoingProxyUsername.text = configuration.proxy.outgoing.username.orNull
			outgoingProxyPassword.text = configuration.proxy.outgoing.password.orNull
			outgoingProxyHttpPort.enabled = true
			outgoingProxyHttpsPort.enabled = true
			outgoingProxyUsername.enabled = true
			outgoingProxyPassword.enabled = true
		}
		configuration.core.pkg.trimToOption.map(simulationPackage.text = _)
		simulationClassName.text = configuration.core.className
		filterStrategies.selection.item = configuration.filters.filterStrategy
		followRedirects.selected = configuration.http.followRedirect
		automaticReferers.selected = configuration.http.automaticReferer
		configuration.filters.patterns.map(filtersTable.addRow(_))
		outputFolderPath.text = configuration.core.outputFolder
		outputEncoding.selection.item = Charset.forName(configuration.core.encoding)
	}

	/**
	 * Reload configuration from the content of the configuration frame
	 * and start recording
	 */
	private def reloadConfigurationAndStart {
		// validate filters
		filtersTable.validateCells

		val props = new RecorderPropertiesBuilder

		// Local proxy
		props.localPort(Try(localProxyHttpPort.text.toInt).getOrElse(0))
		props.localSslPort(Try(localProxyHttpsPort.text.toInt).getOrElse(0))

		// Outgoing proxy
		outgoingProxyHost.text.trimToOption match {
			case Some(host) =>
				props.proxyHost(host)
				props.proxyPort(outgoingProxyHttpPort.text.toInt)
				props.proxySslPort(outgoingProxyHttpsPort.text.toInt)
				outgoingProxyUsername.text.trimToOption.foreach(props.proxyUsername)
				outgoingProxyPassword.text.trimToOption.foreach(props.proxyPassword)

			case None =>
				props.proxyHost("")
				props.proxyPort(0)
				props.proxySslPort(0)
				props.proxyUsername("")
				props.proxyPassword("")
		}

		// Filters
		props.filterStrategy(filterStrategies.selection.item.toString)
		val (patterns, patternTypes) = {
			for (pattern <- filtersTable.getPatterns)
				yield (pattern.pattern, pattern.patternType.toString)
		}.unzip
		props.patterns(patterns)
		props.patternsType(patternTypes)

		// Simulation config
		props.simulationPackage(simulationPackage.text)
		props.simulationClassName(simulationClassName.text.trim)
		props.followRedirect(followRedirects.selected)
		props.automaticReferer(automaticReferers.selected)
		props.simulationOutputFolder(outputFolderPath.text.trim)
		props.encoding(outputEncoding.selection.item.name)

		RecorderConfiguration.reload(props.build)

		if (savePreferences.selected) {
			RecorderConfiguration.saveConfig
		}

		frontend.startRecording
	}
}
