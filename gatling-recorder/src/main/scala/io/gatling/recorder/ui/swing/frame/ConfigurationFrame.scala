/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import java.awt.Font
import javax.swing.filechooser.FileNameExtensionFilter

import scala.collection.JavaConversions.seqAsJavaList
import scala.swing._
import scala.swing.BorderPanel.Position._
import scala.swing.FileChooser.SelectionMode._
import scala.swing.event._
import scala.util.Try

import io.gatling.commons.util.PathHelper._
import io.gatling.commons.util.StringHelper.RichString
import io.gatling.recorder.config._
import io.gatling.recorder.config.FilterStrategy.BlacklistFirst
import io.gatling.recorder.config.RecorderMode.{ Har, Proxy }
import io.gatling.recorder.http.ssl.{ SslServerContext, SslCertUtil, HttpsMode, KeyStoreType }
import io.gatling.recorder.http.ssl.HttpsMode._
import io.gatling.recorder.ui.RecorderFrontend
import io.gatling.recorder.ui.swing.keyReleased
import io.gatling.recorder.ui.swing.Commons._
import io.gatling.recorder.ui.swing.component._
import io.gatling.recorder.ui.swing.frame.ValidationHelper._
import io.gatling.recorder.ui.swing.util._
import io.gatling.recorder.ui.swing.util.UIHelper._

private[swing] class ConfigurationFrame(frontend: RecorderFrontend)(implicit configuration: RecorderConfiguration) extends MainFrame {

  /************************************/
  /**           COMPONENTS           **/
  /************************************/

  /* Top panel components */
  private val modeSelector = new LabelledComboBox[RecorderMode](RecorderMode.AllModes)

  /* Network panel components */
  private val localProxyHttpPort = new TextField(4)
  private val outgoingProxyHost = new TextField(10)
  private val outgoingProxyHttpPort = new TextField(4) { enabled = false }
  private val outgoingProxyHttpsPort = new TextField(4) { enabled = false }
  private val outgoingProxyUsername = new TextField(10) { enabled = false }
  private val outgoingProxyPassword = new PasswordField(10) { enabled = false }

  /* HTTPS mode components */
  private val httpsModes = new LabelledComboBox[HttpsMode](HttpsMode.AllHttpsModes)

  private val keyStoreChooser = new DisplayedSelectionFileChooser(this, 20, Open, selectionMode = FilesOnly)
  private val keyStorePassword = new TextField(10)
  private val keyStoreTypes = new LabelledComboBox[KeyStoreType](KeyStoreType.AllKeyStoreTypes)
  private val certificatePathChooser = new DisplayedSelectionFileChooser(this, 20, Open, selectionMode = FilesOnly)
  private val privateKeyPathChooser = new DisplayedSelectionFileChooser(this, 20, Open, selectionMode = FilesOnly)

  private val caFilesSavePathChooser = new FileChooser { fileSelectionMode = DirectoriesOnly }
  private val generateCAFilesButton = new Button(Action("Generate CA")(caFilesSavePathChooser.saveSelection().foreach { dir =>
    generateCAFiles(dir)
    certificatePathChooser.setPath(s"$dir/${SslServerContext.GatlingCACrtFile}")
    privateKeyPathChooser.setPath(s"$dir/${SslServerContext.GatlingCAKeyFile}")
  }))

  /* Har Panel components */
  private val harFileFilter = new FileNameExtensionFilter("HTTP Archive (.har)", "har")
  private val harPathChooser = new DisplayedSelectionFileChooser(this, 60, Open, selectionMode = FilesOnly, fileFilter = harFileFilter)

  /* Simulation panel components */
  private val simulationPackage = new TextField(30)
  private val simulationClassName = new TextField(30)
  private val followRedirects = new CheckBox("Follow Redirects?")
  private val inferHtmlResources = new CheckBox("Infer html resources?")
  private val removeCacheHeaders = new CheckBox("Remove cache headers?")
  private val checkResponseBodies = new CheckBox("Save & check response bodies?")
  private val automaticReferers = new CheckBox("Automatic Referers?")

  /* Output panel components */
  private val outputEncoding = new ComboBox[String](CharsetHelper.orderedLabelList)
  private val outputFolderChooser = new DisplayedSelectionFileChooser(this, 60, Open, selectionMode = DirectoriesOnly)

  /* Filters panel components */
  private val whiteListTable = new FilterTable("Whitelist")
  private val addWhiteListFilter = Button("+")(whiteListTable.addRow())
  private val removeWhiteListFilter = Button("-")(whiteListTable.removeSelectedRow())
  private val clearWhiteListFilters = Button("Clear")(whiteListTable.removeAllElements())

  private val blackListTable = new FilterTable("Blacklist")
  private val addBlackListFilter = Button("+")(blackListTable.addRow())
  private val removeBlackListFilter = Button("-")(blackListTable.removeSelectedRow())
  private val clearBlackListFilters = Button("Clear")(blackListTable.removeAllElements())
  private val ruleOutStaticResources = Button("No static resources")(blackListStaticResources())

  private val filterStrategies = new LabelledComboBox[FilterStrategy](FilterStrategy.AllStrategies)

  /* Bottom panel components */
  private val savePreferences = new CheckBox("Save preferences") { horizontalTextPosition = Alignment.Left }
  private val start = Button("Start !")(reloadConfigurationAndStart())

  /**********************************/
  /**           UI SETUP           **/
  /**********************************/

  /* Frame setup */
  title = "Gatling Recorder - Configuration"
  resizable = true
  peer.setIconImages(IconList)

  /* Layout setup */
  val root = new BorderPanel {
    /* Top panel: Gatling logo & Recorder mode */
    val top = new BorderPanel {
      val logo = new CenterAlignedFlowPanel { contents += new Label { icon = LogoSmall } }
      val modeSelection = new GridBagPanel {
        border = titledBorder("Recorder mode")
        layout(modeSelector) = new Constraints
      }

      layout(logo) = West
      layout(modeSelection) = East
    }
    /* Center panel: network config or har import, simulation config, output config & filters */
    val center = new BoxPanel(Orientation.Vertical) {
      val network = new BorderPanel {
        border = titledBorder("Network")

        val customKeyStoreConfig = new LeftAlignedFlowPanel {
          contents += new Label("Keystore file: ")
          contents += keyStoreChooser
          contents += new Label("Keystore password: ")
          contents += keyStorePassword
          contents += new Label("Keystore type: ")
          contents += keyStoreTypes
        }

        val certificateAuthorityConfig = new LeftAlignedFlowPanel {
          contents += new Label("CA Certificate: ")
          contents += certificatePathChooser
          contents += new Label("CA Private Key: ")
          contents += privateKeyPathChooser
        }

        val localProxyAndHttpsMode = new LeftAlignedFlowPanel {
          contents += new Label("Listening port*: ")
          contents += new Label("    localhost")
          contents += new Label("HTTP/HTTPS")
          contents += localProxyHttpPort
          contents += new Label("    HTTPS mode: ")
          contents += httpsModes
          contents += generateCAFilesButton
        }

        val outgoingProxy = new LeftAlignedFlowPanel {
          contents += new Label("Outgoing proxy: ")
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

        val httpsModesConfigs = new BoxPanel(Orientation.Vertical) {
          contents += customKeyStoreConfig
          contents += certificateAuthorityConfig
        }

        layout(localProxyAndHttpsMode) = North
        layout(httpsModesConfigs) = Center
        layout(outgoingProxy) = South
      }
      val har = new BorderPanel {
        border = titledBorder("Http Archive (HAR) Import")
        visible = false

        val fileSelection = new LeftAlignedFlowPanel {
          contents += new Label("HAR File: ")
          contents += harPathChooser
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

        val redirectAndInferOptions = new BorderPanel {
          layout(followRedirects) = West
          layout(inferHtmlResources) = East
        }

        val cacheAndResponseBodiesCheck = new BorderPanel {
          layout(removeCacheHeaders) = West
          layout(checkResponseBodies) = East
        }

        layout(config) = North

        layout(redirectAndInferOptions) = West
        layout(automaticReferers) = East
        layout(cacheAndResponseBodiesCheck) = South
      }
      val outputConfig = new BorderPanel {
        border = titledBorder("Output")

        val folderSelection = new LeftAlignedFlowPanel {
          contents += new Label("Output folder*: ")
          contents += outputFolderChooser
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

        val labelAndStrategySelection = new BorderPanel {
          val label = new Label("Java regular expressions that matches the entire URI")
          label.font_=(label.font.deriveFont(Font.PLAIN))
          val strategy = new RightAlignedFlowPanel {
            contents += new Label("Strategy")
            contents += filterStrategies
          }
          layout(label) = West
          layout(strategy) = East
        }

        val whiteList = new BoxPanel(Orientation.Vertical) {
          contents += whiteListTable
          contents += new CenterAlignedFlowPanel {
            contents += addWhiteListFilter
            contents += removeWhiteListFilter
            contents += clearWhiteListFilters
          }
        }

        val blackList = new BoxPanel(Orientation.Vertical) {
          contents += blackListTable
          contents += new CenterAlignedFlowPanel {
            contents += addBlackListFilter
            contents += removeBlackListFilter
            contents += clearBlackListFilters
            contents += ruleOutStaticResources
          }
        }

        val bothLists = new SplitPane(Orientation.Vertical, whiteList, blackList)
        bothLists.resizeWeight = 0.5

        layout(labelAndStrategySelection) = North
        layout(bothLists) = Center
      }

      contents += network
      contents += har
      contents += simulationConfig
      contents += outputConfig
      contents += filters
    }
    /* Bottom panel: Save preferences & start recording/ export HAR */
    val bottom = new RightAlignedFlowPanel {
      contents += savePreferences
      contents += start
    }

    layout(top) = North
    layout(center) = Center
    layout(bottom) = South
  }

  val scrollPane = new ScrollPane(root)

  contents = scrollPane

  centerOnScreen()

  registerValidators()
  populateItemsFromConfiguration()

  /*****************************************/
  /**           EVENTS HANDLING           **/
  /*****************************************/

  def toggleModeSelector(mode: RecorderMode): Unit = mode match {
    case Proxy =>
      root.center.network.visible = true
      root.center.har.visible = false
    case Har =>
      root.center.network.visible = false
      root.center.har.visible = true
  }

  /* Reactions I: handling filters, save checkbox, table edition and switching between Proxy and HAR mode */
  listenTo(filterStrategies.selection, modeSelector.selection, httpsModes.selection, savePreferences)
  // Backticks are needed to match the components, see section 8.1.5 of Scala spec.
  reactions += {
    case SelectionChanged(`modeSelector`) =>
      toggleModeSelector(modeSelector.selection.item)
    case SelectionChanged(`filterStrategies`) =>
      val isNotDisabledStrategy = filterStrategies.selection.item != FilterStrategy.Disabled
      toggleFiltersEdition(isNotDisabledStrategy)
    case SelectionChanged(`httpsModes`) => toggleHttpsModesConfigsVisibility(httpsModes.selection.item)
    case ButtonClicked(`savePreferences`) if !savePreferences.selected =>
      val props = new RecorderPropertiesBuilder
      props.saveConfig(savePreferences.selected)
      RecorderConfiguration.reload(props.build)
      RecorderConfiguration.saveConfig()
  }

  private def toggleFiltersEdition(enabled: Boolean): Unit = {
    whiteListTable.setEnabled(enabled)
    whiteListTable.setFocusable(enabled)
    blackListTable.setEnabled(enabled)
    blackListTable.setFocusable(enabled)
  }

  private def toggleHttpsModesConfigsVisibility(currentMode: HttpsMode) = currentMode match {
    case SelfSignedCertificate =>
      root.center.network.customKeyStoreConfig.visible = false
      root.center.network.certificateAuthorityConfig.visible = false
      generateCAFilesButton.visible = false
    case ProvidedKeyStore =>
      root.center.network.customKeyStoreConfig.visible = true
      root.center.network.certificateAuthorityConfig.visible = false
      generateCAFilesButton.visible = false
    case CertificateAuthority =>
      root.center.network.customKeyStoreConfig.visible = false
      root.center.network.certificateAuthorityConfig.visible = true
      generateCAFilesButton.visible = true
  }

  /* Reactions II: fields validation */
  listenTo(
    localProxyHttpPort.keys,
    keyStoreChooser.chooserKeys,
    keyStorePassword.keys,
    certificatePathChooser.chooserKeys,
    privateKeyPathChooser.chooserKeys,
    harPathChooser.chooserKeys,
    outgoingProxyHost.keys,
    outgoingProxyHttpPort.keys,
    outgoingProxyHttpsPort.keys,
    outputFolderChooser.chooserKeys,
    simulationPackage.keys,
    simulationClassName.keys
  )

  private def registerValidators(): Unit = {

    val keystorePathValidator = (s: String) => selectedHttpsMode != ProvidedKeyStore || isNonEmpty(s)
    val keystorePasswordValidator = (s: String) => selectedHttpsMode != ProvidedKeyStore || isNonEmpty(s)

    val certificatePathValidator = (s: String) => selectedHttpsMode != CertificateAuthority || isNonEmpty(s)
    val privateKeyPathValidator = (s: String) => selectedHttpsMode != CertificateAuthority || isNonEmpty(s)
    val harFilePathValidator = (s: String) => selectedRecorderMode == Proxy || isNonEmpty(s)

    val outgoingProxyPortValidator = (s: String) => outgoingProxyHost.text.isEmpty || isValidPort(s)

    ValidationHelper.registerValidator(localProxyHttpPort, Validator(isValidPort))
    ValidationHelper.registerValidator(keyStoreChooser.textField, Validator(keystorePathValidator))
    ValidationHelper.registerValidator(keyStorePassword, Validator(keystorePasswordValidator))
    ValidationHelper.registerValidator(certificatePathChooser.textField, Validator(certificatePathValidator))
    ValidationHelper.registerValidator(privateKeyPathChooser.textField, Validator(privateKeyPathValidator))
    ValidationHelper.registerValidator(harPathChooser.textField, Validator(harFilePathValidator))
    ValidationHelper.registerValidator(outgoingProxyHost, Validator(isNonEmpty, enableOutgoingProxyConfig, disableOutgoingProxyConfig, alwaysValid = true))
    ValidationHelper.registerValidator(outgoingProxyHttpPort, Validator(outgoingProxyPortValidator))
    ValidationHelper.registerValidator(outgoingProxyHttpsPort, Validator(outgoingProxyPortValidator))
    ValidationHelper.registerValidator(outputFolderChooser.textField, Validator(isNonEmpty))
    ValidationHelper.registerValidator(simulationPackage, Validator(isValidPackageName))
    ValidationHelper.registerValidator(simulationClassName, Validator(isValidSimpleClassName))
  }

  private def enableOutgoingProxyConfig(c: Component): Unit = {
    publish(keyReleased(outgoingProxyHttpPort))
    publish(keyReleased(outgoingProxyHttpsPort))
    outgoingProxyHttpPort.enabled = true
    outgoingProxyHttpsPort.enabled = true
    outgoingProxyUsername.enabled = true
    outgoingProxyPassword.enabled = true
  }

  private def disableOutgoingProxyConfig(c: Component): Unit = {
    outgoingProxyHttpPort.enabled = false
    outgoingProxyHttpsPort.enabled = false
    outgoingProxyUsername.enabled = false
    outgoingProxyPassword.enabled = false
    // hack for validating outgoingProxyHttpPort and outgoingProxyHttpsPort
    outgoingProxyHttpPort.text = ""
    outgoingProxyHttpsPort.text = ""
    outgoingProxyUsername.text = ""
    outgoingProxyPassword.text = ""
    publish(keyReleased(outgoingProxyHttpPort))
    publish(keyReleased(outgoingProxyHttpsPort))
  }

  private def blackListStaticResources(): Unit = {
    List(
      """.*\.js""",
      """.*\.css""",
      """.*\.gif""",
      """.*\.jpeg""",
      """.*\.jpg""",
      """.*\.ico""",
      """.*\.woff""",
      """.*\.(t|o)tf""",
      """.*\.png"""
    ).foreach(blackListTable.addRow)

    filterStrategies.selection.item = BlacklistFirst
  }

  reactions += {
    case KeyReleased(field, _, _, _) =>
      updateValidationStatus(field.asInstanceOf[TextField])
      start.enabled = ValidationHelper.validationStatus
  }

  def selectedRecorderMode = modeSelector.selection.item

  private def selectedHttpsMode = httpsModes.selection.item

  def harFilePath = harPathChooser.selection

  def updateHarFilePath(path: Option[String]): Unit = path.foreach(p => harPathChooser.setPath(p))

  def generateCAFiles(directory: String): Unit = {
    SslCertUtil.generateGatlingCAPEMFiles(
      directory,
      SslServerContext.GatlingCAKeyFile,
      SslServerContext.GatlingCACrtFile
    )

    Dialog.showMessage(
      title = "Download successful",
      message =
      s"""|Gatling's CA certificate and key were successfully saved to
           |$directory .""".stripMargin
    )
  }

  /****************************************/
  /**           CONFIGURATION            **/
  /****************************************/

  /**
   * Configure fields, checkboxes, filters... based on the current Recorder configuration
   */
  private def populateItemsFromConfiguration(): Unit = {

    modeSelector.selection.item = configuration.core.mode
    toggleModeSelector(modeSelector.selection.item)

    configuration.core.harFilePath.foreach(harPathChooser.setPath)

    localProxyHttpPort.text = configuration.proxy.port.toString

    httpsModes.selection.item = configuration.proxy.https.mode
    toggleHttpsModesConfigsVisibility(selectedHttpsMode)

    keyStoreChooser.setPath(configuration.proxy.https.keyStore.path)
    keyStorePassword.text = configuration.proxy.https.keyStore.password
    keyStoreTypes.selection.item = configuration.proxy.https.keyStore.keyStoreType

    certificatePathChooser.setPath(configuration.proxy.https.certificateAuthority.certificatePath)
    privateKeyPathChooser.setPath(configuration.proxy.https.certificateAuthority.privateKeyPath)

    configuration.proxy.outgoing.host.foreach { proxyHost =>
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
    inferHtmlResources.selected = configuration.http.inferHtmlResources
    removeCacheHeaders.selected = configuration.http.removeCacheHeaders
    checkResponseBodies.selected = configuration.http.checkResponseBodies
    automaticReferers.selected = configuration.http.automaticReferer
    configuration.filters.blackList.patterns.foreach(blackListTable.addRow)
    configuration.filters.whiteList.patterns.foreach(whiteListTable.addRow)
    outputFolderChooser.setPath(configuration.core.outputFolder)
    outputEncoding.selection.item = CharsetHelper.charsetNameToLabel(configuration.core.encoding)
    savePreferences.selected = configuration.core.saveConfig

  }

  /**
   * Reload configuration from the content of the configuration frame
   * and start recording
   */
  private def reloadConfigurationAndStart(): Unit = {
    // clean up filters
    whiteListTable.cleanUp()
    blackListTable.cleanUp()

    val filterValidationFailures =
      if (filterStrategies.selection.item == FilterStrategy.Disabled)
        Nil
      else
        whiteListTable.validate ::: blackListTable.validate

    if (filterValidationFailures.nonEmpty) {
      frontend.handleFilterValidationFailures(filterValidationFailures)

    } else {
      val props = new RecorderPropertiesBuilder

      props.mode(modeSelector.selection.item)

      props.harFilePath(harPathChooser.selection)

      // Local proxy
      props.localPort(Try(localProxyHttpPort.text.toInt).getOrElse(0))

      props.httpsMode(httpsModes.selection.item.toString)

      props.keystorePath(keyStoreChooser.selection)
      props.keyStorePassword(keyStorePassword.text)
      props.keyStoreType(keyStoreTypes.selection.item.toString)

      props.certificatePath(certificatePathChooser.selection)
      props.privateKeyPath(privateKeyPathChooser.selection)

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
      props.whitelist(whiteListTable.getRegexs)
      props.blacklist(blackListTable.getRegexs)

      // Simulation config
      props.simulationPackage(simulationPackage.text)
      props.simulationClassName(simulationClassName.text.trim)
      props.followRedirect(followRedirects.selected)
      props.inferHtmlResources(inferHtmlResources.selected)
      props.removeCacheHeaders(removeCacheHeaders.selected)
      props.checkResponseBodies(checkResponseBodies.selected)
      props.automaticReferer(automaticReferers.selected)
      props.simulationOutputFolder(outputFolderChooser.selection.trim)
      props.encoding(CharsetHelper.labelToCharsetName(outputEncoding.selection.item))
      props.saveConfig(savePreferences.selected)

      RecorderConfiguration.reload(props.build)

      if (savePreferences.selected) {
        RecorderConfiguration.saveConfig()
      }

      if (ValidationHelper.allValid) frontend.startRecording()
    }
  }
}
