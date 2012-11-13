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

import java.awt.{ FlowLayout, FileDialog, Dimension, BorderLayout }
import java.awt.event.{ ItemListener, ItemEvent, ActionListener, ActionEvent }
import java.nio.charset.Charset
import scala.collection.JavaConversions.{ seqAsJavaList, collectionAsScalaIterable }
import com.excilys.ebi.gatling.recorder.config.Configuration
import com.excilys.ebi.gatling.recorder.config.Configuration.configuration
import com.excilys.ebi.gatling.recorder.controller.RecorderController
import com.excilys.ebi.gatling.recorder.ui.Commons
import com.excilys.ebi.gatling.recorder.ui.Commons.iconList
import com.excilys.ebi.gatling.recorder.ui.component.{ SaveConfigurationListener, FilterTable }
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterStrategy
import com.excilys.ebi.gatling.recorder.ui.frame.ValidationHelper.{ proxyHostValidator, nonEmptyValidator, intValidator }
import grizzled.slf4j.Logging
import javax.swing.{ SwingConstants, JTextField, JPanel, JLabel, JFrame, JFileChooser, JComboBox, JCheckBox, JButton, BorderFactory }
import com.excilys.ebi.gatling.recorder.ui.util.ScalaSwing

class ConfigurationFrame(controller: RecorderController) extends JFrame with ScalaSwing with Logging {

	private val IS_MAC_OSX = System.getProperty("os.name").startsWith("Mac");

	val txtPort = new JTextField(null, 4)
	val txtSslPort = new JTextField(null, 4)

	val txtProxyHost = new JTextField(null, 15)
	val txtProxyPort = new JTextField(null, 4)
	txtProxyPort.setEnabled(false)
	val txtProxySslPort = new JTextField(null, 4)
	txtProxySslPort.setEnabled(false)
	val txtProxyUsername = new JTextField(null, 12)
	txtProxyUsername.setEnabled(false)
	val txtProxyPassword = new JTextField(null, 12)
	txtProxyPassword.setEnabled(false)

	val cbFilterStrategies = new JComboBox
	val chkSavePref = new JCheckBox("Save preferences")
	val chkFollowRedirect = new JCheckBox("Follow Redirects?")
	val chkAutomaticReferer = new JCheckBox("Automatic Referers?")
	val txtOutputFolder = new JTextField(66)
	val tblFilters = new FilterTable
	val cbOutputEncoding = new JComboBox
	val txtSimulationPackage = new JTextField(30)
	val txtSimulationClassName = new JTextField(30)

	private val btnFiltersAdd = new JButton("+")
	private val btnFiltersDel = new JButton("-")
	private val btnOutputFolder = new JButton("Browse")
	private val btnClear = new JButton("Clear")
	val btnStart = new JButton("Start !")

	private var pnlTop: JPanel = null
	private var pnlCenter: JPanel = null
	private var pnlBottom: JPanel = null

	private var fileDialog: FileDialog = null
	private var fileChooser: JFileChooser = null

	/** Initialization of the frame **/

	setTitle("Gatling Recorder - Configuration")
	setLayout(new BorderLayout)
	setMinimumSize(new Dimension(1024, 768))
	setResizable(true)
	setLocationRelativeTo(null)
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
	setIconImages(iconList)

	/** Initialization of components **/
	initTopPanel
	initCenterPanel
	initBottomPanel
	initOutputDirectoryChooser

	setListeners

	setValidationListeners

	populateItemsFromConfiguration(configuration)

	private def initOutputDirectoryChooser {

		if (IS_MAC_OSX) {
			// on mac, use native dialog because JFileChooser is buggy
			System.setProperty("apple.awt.fileDialogForDirectories", "true")
			fileDialog = new FileDialog(ConfigurationFrame.this)

		} else {
			fileChooser = new JFileChooser
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
		}
	}

	private def initTopPanel {
		/***** Creating Top Panel (Network) *****/
		pnlTop = new JPanel(new BorderLayout)

		/* Gatling Image */
		val pnlImage = new JPanel
		pnlImage.add(new JLabel(Commons.logoSmall))

		/* Network Panel */
		val pnlNetwork = new JPanel(new BorderLayout)
		pnlNetwork.setBorder(BorderFactory.createTitledBorder("Network"))
		pnlNetwork.setLayout(new BorderLayout)

		/* Local proxy host panel */
		val localProxyHostPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		localProxyHostPanel.add(new JLabel("Listening port* : "))
		localProxyHostPanel.add(new JLabel("                                    localhost"))

		/* Local proxy ports panel */
		val localProxyPortsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT))
		localProxyPortsPanel.add(new JLabel("HTTP"))
		localProxyPortsPanel.add(txtPort)
		localProxyPortsPanel.add(new JLabel("HTTPS"))
		localProxyPortsPanel.add(txtSslPort)

		/* Local proxy panel */
		val localProxyPanel = new JPanel(new FlowLayout)
		localProxyPanel.add(localProxyHostPanel)
		localProxyPanel.add(localProxyPortsPanel)

		/* Outgoing proxy host panel */
		val outgoingProxyHostPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		outgoingProxyHostPanel.add(new JLabel("Outgoing proxy : "))
		outgoingProxyHostPanel.add(new JLabel("host:"))
		outgoingProxyHostPanel.add(txtProxyHost)
		outgoingProxyHostPanel.add(new JLabel("HTTP"))
		outgoingProxyHostPanel.add(txtProxyPort)
		outgoingProxyHostPanel.add(new JLabel("HTTPS"))
		outgoingProxyHostPanel.add(txtProxySslPort)

		/* Outgoing proxy ports panel */
		val outgoingProxyCredentialsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT))
		outgoingProxyCredentialsPanel.add(new JLabel("Username"))
		outgoingProxyCredentialsPanel.add(txtProxyUsername)
		outgoingProxyCredentialsPanel.add(new JLabel("Password"))
		outgoingProxyCredentialsPanel.add(txtProxyPassword)

		/* Outgoing proxy panel */
		val outgoingProxyPanel = new JPanel(new BorderLayout)
		outgoingProxyPanel.add(outgoingProxyHostPanel, BorderLayout.NORTH)
		outgoingProxyPanel.add(outgoingProxyCredentialsPanel, BorderLayout.SOUTH)

		/* Adding panels to newtworkPanel */
		pnlNetwork.add(localProxyPanel, BorderLayout.NORTH)
		pnlNetwork.add(outgoingProxyPanel, BorderLayout.SOUTH)

		/* Adding Image and network panel to top panel */
		pnlTop.add(pnlImage, BorderLayout.WEST)
		pnlTop.add(pnlNetwork, BorderLayout.EAST)

		/* Adding panel to Frame */
		add(pnlTop, BorderLayout.NORTH)
	}

	private def initCenterPanel {
		/***** Creating Center Panel (Output + Start) *****/
		pnlCenter = new JPanel
		pnlCenter.setLayout(new BorderLayout)

		/* Output Folder Panel */
		val outputFolderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		outputFolderPanel.add(new JLabel("Output folder* : "))
		outputFolderPanel.add(txtOutputFolder)
		outputFolderPanel.add(btnOutputFolder)

		for (c <- Charset.availableCharsets.values)
			cbOutputEncoding.addItem(c)

		/* Output Panel */
		val outputPanel = new JPanel(new BorderLayout)
		outputPanel.setBorder(BorderFactory.createTitledBorder("Output"))

		val outputFormatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		outputFormatPanel.add(new JLabel("Encoding: "))
		outputFormatPanel.add(cbOutputEncoding)

		outputPanel.add(outputFolderPanel, BorderLayout.NORTH)
		outputPanel.add(outputFormatPanel, BorderLayout.CENTER)

		/* Simulation information panel */
		val simulationInfoPanel = new JPanel(new BorderLayout)

		val packageNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		packageNamePanel.add(new JLabel("Package: "))
		packageNamePanel.add(txtSimulationPackage)

		val simulationNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		simulationNamePanel.add(new JLabel("Class Name*: "))
		simulationNamePanel.add(txtSimulationClassName)

		simulationInfoPanel.add(packageNamePanel, BorderLayout.WEST)
		simulationInfoPanel.add(simulationNamePanel, BorderLayout.EAST)

		val simulationConfigPanel = new JPanel(new BorderLayout)
		simulationConfigPanel.setBorder(BorderFactory.createTitledBorder("Simulation Information"))

		simulationConfigPanel.add(simulationInfoPanel, BorderLayout.NORTH)
		simulationConfigPanel.add(chkFollowRedirect, BorderLayout.WEST)
		simulationConfigPanel.add(chkAutomaticReferer, BorderLayout.EAST)

		/* Filters Panel */
		val filtersPanel = new JPanel(new BorderLayout);
		filtersPanel.setBorder(BorderFactory.createTitledBorder("Filters"))

		// Fill Combo Box for Strategies
		for (ft <- FilterStrategy.values)
			cbFilterStrategies.addItem(ft)

		/* Filter Actions panel */
		val filterActionsPanel = new JPanel
		filterActionsPanel.add(new JLabel("Strategy"))
		filterActionsPanel.add(cbFilterStrategies)
		filterActionsPanel.add(btnFiltersAdd)
		filterActionsPanel.add(btnFiltersDel)
		filterActionsPanel.add(btnClear)

		/* Adding panels to filterPanel */
		filtersPanel.add(tblFilters, BorderLayout.CENTER)
		filtersPanel.add(filterActionsPanel, BorderLayout.SOUTH)

		/* Adding panels to bottomPanel */
		pnlCenter.add(simulationConfigPanel, BorderLayout.NORTH)
		pnlCenter.add(outputPanel, BorderLayout.CENTER)
		pnlCenter.add(filtersPanel, BorderLayout.SOUTH)

		/* Adding panel to Frame */
		add(pnlCenter, BorderLayout.CENTER)
	}

	private def initBottomPanel {
		/***** Creating Bottom Panel (Filters) *****/
		pnlBottom = new JPanel
		pnlBottom.setLayout(new BorderLayout)

		/* Start Action Panel */
		val startActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT))
		startActionPanel.add(chkSavePref)
		startActionPanel.add(btnStart)

		chkSavePref.setHorizontalTextPosition(SwingConstants.LEFT);

		pnlBottom.add(startActionPanel, BorderLayout.SOUTH);

		/* Adding panel to Frame */
		add(pnlBottom, BorderLayout.SOUTH)
	}

	private def setListeners {
		// Enables or disables filter edition depending on the selected strategy
		cbFilterStrategies.addItemListener((e: ItemEvent) => {
			if (e.getStateChange == ItemEvent.SELECTED && e.getItem == FilterStrategy.NONE) {
				tblFilters.setEnabled(false)
				tblFilters.setFocusable(false)
			} else {
				tblFilters.setEnabled(true)
				tblFilters.setFocusable(true)
			}
		})

		// Adds a filter row when + button clicked
		btnFiltersAdd.addActionListener((e: ActionEvent) => tblFilters.addRow)

		// Removes selected filter when - button clicked
		btnFiltersDel.addActionListener((e: ActionEvent) => tblFilters.removeSelectedRow)

		// Removes all filters when clear button clicked
		btnClear.addActionListener((e: ActionEvent) => tblFilters.removeAllElements)

		// Opens a save dialog when Browse button clicked
		btnOutputFolder.addActionListener((e: ActionEvent) => {

			var chosenDirPath: String = null

			if (IS_MAC_OSX) {
				fileDialog.setVisible(true)

				if (fileDialog.getDirectory == null)
					return

				chosenDirPath = fileDialog.getDirectory + fileDialog.getFile

			} else {
				if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
					return

				chosenDirPath = fileChooser.getSelectedFile.getPath
			}

			txtOutputFolder.setText(chosenDirPath)
		})

		// Validates form when Start button clicked
		btnStart.addActionListener(new SaveConfigurationListener(controller, this))
	}

	private def setValidationListeners {
		txtPort.addKeyListener(intValidator(this, "port"))
		txtSslPort.addKeyListener(intValidator(this, "sslPort"))
		txtProxyHost.addKeyListener(proxyHostValidator(this))
		txtProxyPort.addKeyListener(intValidator(this, "proxyPort"))
		txtProxySslPort.addKeyListener(intValidator(this, "proxySslPort"))
		txtOutputFolder.addKeyListener(nonEmptyValidator(this, "outputFolder"))
		txtSimulationClassName.addKeyListener(nonEmptyValidator(this, "simulationClassName"))
	}

	def populateItemsFromConfiguration(configuration: Configuration) {
		txtPort.setText(configuration.port.toString)
		txtSslPort.setText(configuration.sslPort.toString)

		configuration.proxy.host.map { proxyHost =>
			txtProxyHost.setText(proxyHost)
			txtProxyPort.setText(configuration.proxy.port.getOrElse(0).toString)
			txtProxySslPort.setText(configuration.proxy.sslPort.getOrElse(0).toString)
			txtProxyUsername.setText(configuration.proxy.getUsername.getOrElse(null))
			txtProxyPassword.setText(configuration.proxy.getPassword.getOrElse(null))
			txtProxyPort.setEnabled(true)
			txtProxySslPort.setEnabled(true)
			txtProxyUsername.setEnabled(true)
			txtProxyPassword.setEnabled(true)
		}
		configuration.simulationPackage.map(txtSimulationPackage.setText(_))
		txtSimulationClassName.setText(configuration.simulationClassName)
		cbFilterStrategies.setSelectedItem(configuration.filterStrategy)
		chkFollowRedirect.setSelected(configuration.followRedirect)
		chkAutomaticReferer.setSelected(configuration.automaticReferer)
		for (pattern <- configuration.patterns)
			tblFilters.addRow(pattern)
		txtOutputFolder.setText(configuration.outputFolder)
		chkSavePref.setSelected(configuration.saveConfiguration)
		cbOutputEncoding.setSelectedItem(Charset.forName(configuration.encoding))
	}
}