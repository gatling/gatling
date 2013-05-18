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

import java.awt.{ BorderLayout, Dimension, FileDialog, FlowLayout }
import java.awt.event.{ ActionEvent, ItemEvent }
import java.nio.charset.Charset

import scala.collection.JavaConversions.{ collectionAsScalaIterable, seqAsJavaList }

import io.gatling.core.util.StringHelper.RichString
import io.gatling.recorder.config.RecorderConfiguration.{ configuration, saveConfiguration }
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.ui.Commons
import io.gatling.recorder.ui.Commons.iconList
import io.gatling.recorder.ui.component.{ FilterTable, SaveConfigurationListener }
import io.gatling.recorder.ui.enumeration.FilterStrategy
import io.gatling.recorder.ui.frame.ConfigurationFrame.{ harMode, httpMode }
import io.gatling.recorder.ui.frame.ValidationHelper.{ intValidator, nonEmptyValidator, proxyHostValidator }
import io.gatling.recorder.ui.util.ScalaSwing

import javax.swing._

object ConfigurationFrame {
	val httpMode = "HTTP Proxy"
	val harMode = "HAR Converter"
}
class ConfigurationFrame(controller: RecorderController) extends JFrame with ScalaSwing {

	type Chooser = Either[FileDialog, JFileChooser]

	private val IS_MAC_OSX = System.getProperty("os.name").startsWith("Mac")

	val txtPort = new JTextField(4)
	val txtSslPort = new JTextField(4)

	val txtProxyHost = new JTextField(12)
	val txtProxyPort = new JTextField(4)
	txtProxyPort.setEnabled(false)
	val txtProxySslPort = new JTextField(4)
	txtProxySslPort.setEnabled(false)
	val txtProxyUsername = new JTextField(10)
	txtProxyUsername.setEnabled(false)
	val txtProxyPassword = new JTextField(10)
	txtProxyPassword.setEnabled(false)

	val cbFilterStrategies = new JComboBox[FilterStrategy.Value]
	val chkSavePref = new JCheckBox("Save preferences")
	val chkFollowRedirect = new JCheckBox("Follow Redirects?")
	val chkAutomaticReferer = new JCheckBox("Automatic Referers?")
	val txtHarFile = new JTextField(66)
	val txtOutputFolder = new JTextField(66)
	val tblFilters = new FilterTable
	val cbOutputEncoding = new JComboBox[Charset]
	val txtSimulationPackage = new JTextField(30)
	val txtSimulationClassName = new JTextField(30)

	private val btnFiltersAdd = new JButton("+")
	private val btnFiltersDel = new JButton("-")
	private val btnHarFile = new JButton("Browse")
	private val btnOutputFolder = new JButton("Browse")
	private val btnClear = new JButton("Clear")
	val btnStart = new JButton("Start !")

	private var pnlTop: JPanel = _
	private var pnlCenter: JPanel = _
	private var pnlBottom: JPanel = _
	private var networkPanel: JPanel = _
	private var harPanel: JPanel = _

	private val harFileChooser = initChooser(JFileChooser.FILES_ONLY)
	private val outputFolderChooser = initChooser(JFileChooser.DIRECTORIES_ONLY)

	var modeSelector: JComboBox[String] = _

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

	setListeners

	setValidationListeners

	populateItemsFromConfiguration

	if (IS_MAC_OSX) {
		// on mac, use native dialog because JFileChooser is buggy
		System.setProperty("apple.awt.fileDialogForDirectories", "true")
	}

	private def initChooser(mode: Int): Chooser = {
		if (IS_MAC_OSX) {
			Left(new FileDialog(ConfigurationFrame.this))
		} else {
			val fileChooser = new JFileChooser
			fileChooser.setFileSelectionMode(mode)
			Right(fileChooser)
		}
	}

	private def initTopPanel {
		/***** Creating Top Panel (Network) *****/
		pnlTop = new JPanel(new BorderLayout)

		/* Gatling Image */
		val pnlImage = new JPanel
		pnlImage.add(new JLabel(Commons.logoSmall))

		/* Mode selection dropdown */
		modeSelector = new JComboBox[String]()
		modeSelector.addItem(httpMode)
		modeSelector.addItem(harMode)
		modeSelector.setSelectedIndex(0)

		/* Mode selection panel */
		val modeSelectionPanel = new JPanel(new BorderLayout)
		modeSelectionPanel.setBorder(BorderFactory.createTitledBorder("Recorder mode"))
		modeSelectionPanel.add(modeSelector)

		/* Adding Image and network panel to top panel */
		pnlTop.add(pnlImage, BorderLayout.WEST)
		pnlTop.add(modeSelectionPanel, BorderLayout.EAST)

		/* Adding panel to Frame */
		add(pnlTop, BorderLayout.NORTH)
	}

	private def initCenterPanel {
		/***** Creating Center Panel (Output + Start) *****/
		pnlCenter = new JPanel
		pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS))

		/* Network Panel */
		networkPanel = new JPanel(new BorderLayout)
		networkPanel.setBorder(BorderFactory.createTitledBorder("Network"))

		/* Local proxy host panel */
		val localProxyHostPanel = new JPanel
		localProxyHostPanel.setLayout(new BoxLayout(localProxyHostPanel, BoxLayout.X_AXIS))
		localProxyHostPanel.add(new JLabel("Listening port* : "))
		localProxyHostPanel.add(new JLabel("    localhost"))

		/* Local proxy ports panel */
		val localProxyPortsPanel = new JPanel
		localProxyPortsPanel.add(new JLabel("HTTP"))
		localProxyPortsPanel.add(txtPort)
		localProxyPortsPanel.add(new JLabel("HTTPS"))
		localProxyPortsPanel.add(txtSslPort)

		/* Local proxy panel */
		val localProxyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		localProxyPanel.add(localProxyHostPanel)
		localProxyPanel.add(localProxyPortsPanel)

		/* Outgoing proxy host panel */
		val outgoingProxyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		outgoingProxyPanel.add(new JLabel("Outgoing proxy : "))
		outgoingProxyPanel.add(new JLabel("host:"))
		outgoingProxyPanel.add(txtProxyHost)
		outgoingProxyPanel.add(new JLabel("HTTP"))
		outgoingProxyPanel.add(txtProxyPort)
		outgoingProxyPanel.add(new JLabel("HTTPS"))
		outgoingProxyPanel.add(txtProxySslPort)
		outgoingProxyPanel.add(new JLabel("Username"))
		outgoingProxyPanel.add(txtProxyUsername)
		outgoingProxyPanel.add(new JLabel("Password"))
		outgoingProxyPanel.add(txtProxyPassword)

		/* Adding panels to newtworkPanel */
		networkPanel.add(localProxyPanel, BorderLayout.NORTH)
		networkPanel.add(outgoingProxyPanel, BorderLayout.SOUTH)

		/* Output Folder Panel */
		val outputFolderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		outputFolderPanel.add(new JLabel("Output folder* : "))
		outputFolderPanel.add(txtOutputFolder)
		outputFolderPanel.add(btnOutputFolder)

		for (c <- Charset.availableCharsets.values)
			cbOutputEncoding.addItem(c)

		/* HAR File Panel */
		val harFilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
		harFilePanel.add(new JLabel("HAR File : "))
		harFilePanel.add(txtHarFile)
		harFilePanel.add(btnHarFile)

		/* HAR Panel : not visible when starting*/
		harPanel = new JPanel(new BorderLayout)
		harPanel.setVisible(false)
		harPanel.setBorder(BorderFactory.createTitledBorder("Http Archive (HAR) Import"))
		harPanel.add(harFilePanel)

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
		val filtersPanel = new JPanel(new BorderLayout)
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
		pnlCenter.add(networkPanel)
		pnlCenter.add(harPanel)
		pnlCenter.add(simulationConfigPanel)
		pnlCenter.add(outputPanel)
		pnlCenter.add(filtersPanel)

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

		chkSavePref.setHorizontalTextPosition(SwingConstants.LEFT)

		pnlBottom.add(startActionPanel, BorderLayout.SOUTH)

		/* Adding panel to Frame */
		add(pnlBottom, BorderLayout.SOUTH)
	}

	private def setListeners {
		// Change panel depending on what mode is selected
		modeSelector.addActionListener { e: ActionEvent =>
			{
				val selectedMode = modeSelector.getItemAt(modeSelector.getSelectedIndex)
				if (selectedMode == httpMode) {
					networkPanel.setVisible(true)
					harPanel.setVisible(false)
				} else {
					networkPanel.setVisible(false)
					harPanel.setVisible(true)
				}
			}
		}
		// Enables or disables filter edition depending on the selected strategy
		cbFilterStrategies.addItemListener { e: ItemEvent =>
			if (e.getStateChange == ItemEvent.SELECTED && e.getItem == FilterStrategy.NONE) {
				tblFilters.setEnabled(false)
				tblFilters.setFocusable(false)
			} else {
				tblFilters.setEnabled(true)
				tblFilters.setFocusable(true)
			}
		}

		// Adds a filter row when + button clicked
		btnFiltersAdd.addActionListener { e: ActionEvent => tblFilters.addRow }

		// Removes selected filter when - button clicked
		btnFiltersDel.addActionListener { e: ActionEvent => tblFilters.removeSelectedRow }

		// Removes all filters when clear button clicked
		btnClear.addActionListener { e: ActionEvent => tblFilters.removeAllElements }

		// Opens a save dialog when Browse button clicked
		btnOutputFolder.addActionListener { _: ActionEvent => getPath(outputFolderChooser).foreach(txtOutputFolder.setText) }

		btnHarFile.addActionListener { _: ActionEvent => getPath(harFileChooser).foreach(txtHarFile.setText) }

		// Validates form when Start button clicked
		btnStart.addActionListener(new SaveConfigurationListener(controller, this))
	}

	private def getPath(chooser: Chooser): Option[String] = chooser match {
		case Left(fileDialog) =>
			fileDialog.setVisible(true)
			Option(fileDialog.getDirectory).map(_ + fileDialog.getFile)

		case Right(fileChooser) =>
			if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
				None
			else
				Some(fileChooser.getSelectedFile.getPath)
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

	def populateItemsFromConfiguration {
		txtPort.setText(configuration.proxy.port.toString)
		txtSslPort.setText(configuration.proxy.sslPort.toString)

		configuration.proxy.outgoing.host.map { proxyHost =>
			txtProxyHost.setText(proxyHost)
			txtProxyPort.setText(configuration.proxy.outgoing.port.getOrElse(0).toString)
			txtProxySslPort.setText(configuration.proxy.outgoing.sslPort.getOrElse(0).toString)
			txtProxyUsername.setText(configuration.proxy.outgoing.username.getOrElse(null))
			txtProxyPassword.setText(configuration.proxy.outgoing.password.getOrElse(null))
			txtProxyPort.setEnabled(true)
			txtProxySslPort.setEnabled(true)
			txtProxyUsername.setEnabled(true)
			txtProxyPassword.setEnabled(true)
		}
		configuration.core.pkg.trimToOption.map(txtSimulationPackage.setText)
		txtSimulationClassName.setText(configuration.core.className)
		cbFilterStrategies.setSelectedItem(configuration.filters.filterStrategy)
		chkFollowRedirect.setSelected(configuration.http.followRedirect)
		chkAutomaticReferer.setSelected(configuration.http.automaticReferer)
		for (pattern <- configuration.filters.patterns)
			tblFilters.addRow(pattern)
		txtOutputFolder.setText(configuration.core.outputFolder)
		chkSavePref.setSelected(saveConfiguration)
		cbOutputEncoding.setSelectedItem(Charset.forName(configuration.core.encoding))
	}
}