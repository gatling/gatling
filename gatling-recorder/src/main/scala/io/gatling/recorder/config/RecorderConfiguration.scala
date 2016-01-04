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
package io.gatling.recorder.config

import java.io.FileNotFoundException
import java.nio.file.Path

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.duration.{ Duration, DurationInt }
import scala.util.Properties.userHome

import io.gatling.commons.util.ConfigHelper.configChain
import io.gatling.commons.util.Io._
import io.gatling.commons.util.PathHelper._
import io.gatling.commons.util.StringHelper.RichString
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingFiles._
import io.gatling.core.filter.{ BlackList, Filters, WhiteList }
import io.gatling.recorder.http.ssl.{ KeyStoreType, HttpsMode }

import com.typesafe.config.{ ConfigFactory, Config, ConfigRenderOptions }
import com.typesafe.scalalogging.StrictLogging

import scala.util.control.NonFatal

private[recorder] object RecorderConfiguration extends StrictLogging {

  implicit class IntOption(val value: Int) extends AnyVal {
    def toOption = if (value != 0) Some(value) else None
  }

  val Remove4SpacesRegex = """\s{4}""".r

  val RenderOptions = ConfigRenderOptions.concise.setFormatted(true).setJson(false)

  var configFile: Option[Path] = None

  implicit var configuration: RecorderConfiguration = _

  implicit val gatlingConfiguration = GatlingConfiguration.load()

  private[this] def getClassLoader = Thread.currentThread.getContextClassLoader
  private[this] def getDefaultConfig(classLoader: ClassLoader) =
    ConfigFactory.parseResources(classLoader, "recorder-defaults.conf")

  def fakeConfig(props: mutable.Map[String, _ <: Any]): RecorderConfiguration = {
    val defaultConfig = getDefaultConfig(getClassLoader)
    buildConfig(configChain(ConfigFactory.parseMap(props), defaultConfig))
  }

  def initialSetup(props: mutable.Map[String, _ <: Any], recorderConfigFile: Option[Path] = None): Unit = {
    val classLoader = getClassLoader
    val defaultConfig = getDefaultConfig(classLoader)
    configFile = recorderConfigFile.orElse(Option(classLoader.getResource("recorder.conf")).map(url => url.toURI))

    val customConfig = configFile.map(path => ConfigFactory.parseFile(path.toFile)).getOrElse {
      // Should only happens with a manually (and incorrectly) updated Maven archetype or SBT template
      println("recorder.conf file couldn't be located or is outdated")
      println("Recorder preferences won't be saved.")
      println("""If running from sbt, please run "copyConfigFiles" and check the plugin documentation.""")
      ConfigFactory.empty
    }
    val propertiesConfig = ConfigFactory.parseMap(props)

    try {
      configuration = buildConfig(configChain(ConfigFactory.systemProperties, propertiesConfig, customConfig, defaultConfig))
    } catch {
      case NonFatal(e) =>
        logger.warn(s"Loading configuration crashed: ${e.getMessage}. Probable cause is a format change, resetting.")
        configFile.foreach(_.delete())
        configuration = buildConfig(configChain(ConfigFactory.systemProperties, propertiesConfig, defaultConfig))
    }
  }

  def reload(props: mutable.Map[String, _ <: Any]): Unit = {
    val frameConfig = ConfigFactory.parseMap(props)
    configuration = buildConfig(configChain(frameConfig, configuration.config))
  }

  def saveConfig(): Unit = {
    // Remove request bodies folder configuration (transient), keep only Gatling-related properties
    val configToSave = configuration.config.withoutPath(ConfigKeys.core.BodiesFolder).root.withOnlyKey(ConfigKeys.ConfigRoot)
    configFile.foreach(file => withCloseable(createAndOpen(file).writer(gatlingConfiguration.core.charset))(_.write(configToSave.render(RenderOptions))))
  }

  private[config] def createAndOpen(path: Path): Path =
    if (!path.exists) {
      val parent = path.getParent
      if (parent.exists) path.touch
      else throw new FileNotFoundException(s"Directory '${parent.toString}' for recorder configuration does not exist")
    } else path

  private def buildConfig(config: Config): RecorderConfiguration = {
    import ConfigKeys._

      def getOutputFolder(folder: String) = {
        folder.trimToOption match {
          case Some(f)                               => f
          case _ if sys.env.contains("GATLING_HOME") => sourcesDirectory.toFile.toString
          case _                                     => userHome
        }
      }

      def getBodiesFolder =
        if (config.hasPath(core.BodiesFolder)) config.getString(core.BodiesFolder)
        else bodiesDirectory.toFile.toString

    RecorderConfiguration(
      core = CoreConfiguration(
        mode = RecorderMode(config.getString(core.Mode)),
        encoding = config.getString(core.Encoding),
        outputFolder = getOutputFolder(config.getString(core.SimulationOutputFolder)),
        bodiesFolder = getBodiesFolder,
        pkg = config.getString(core.Package),
        className = config.getString(core.ClassName),
        thresholdForPauseCreation = config.getInt(core.ThresholdForPauseCreation) milliseconds,
        saveConfig = config.getBoolean(core.SaveConfig),
        headless = config.getBoolean(core.Headless),
        harFilePath = config.getString(core.HarFilePath).trimToOption
      ),
      filters = FiltersConfiguration(
        filterStrategy = FilterStrategy(config.getString(filters.FilterStrategy)),
        whiteList = WhiteList(config.getStringList(filters.WhitelistPatterns).toList),
        blackList = BlackList(config.getStringList(filters.BlacklistPatterns).toList)
      ),
      http = HttpConfiguration(
        automaticReferer = config.getBoolean(http.AutomaticReferer),
        followRedirect = config.getBoolean(http.FollowRedirect),
        inferHtmlResources = config.getBoolean(http.InferHtmlResources),
        removeCacheHeaders = config.getBoolean(http.RemoveCacheHeaders),
        checkResponseBodies = config.getBoolean(http.CheckResponseBodies)
      ),
      proxy = ProxyConfiguration(
        port = config.getInt(proxy.Port),
        https = HttpsModeConfiguration(
          mode = HttpsMode(config.getString(proxy.https.Mode)),
          keyStore = KeyStoreConfiguration(
            path = config.getString(proxy.https.keyStore.Path),
            password = config.getString(proxy.https.keyStore.Password),
            keyStoreType = KeyStoreType(config.getString(proxy.https.keyStore.Type))
          ),
          certificateAuthority = CertificateAuthorityConfiguration(
            certificatePath = config.getString(proxy.https.certificateAuthority.CertificatePath),
            privateKeyPath = config.getString(proxy.https.certificateAuthority.PrivateKeyPath)
          )
        ),
        outgoing = OutgoingProxyConfiguration(
          host = config.getString(proxy.outgoing.Host).trimToOption,
          username = config.getString(proxy.outgoing.Username).trimToOption,
          password = config.getString(proxy.outgoing.Password).trimToOption,
          port = config.getInt(proxy.outgoing.Port).toOption,
          sslPort = config.getInt(proxy.outgoing.SslPort).toOption
        )
      ),
      netty = NettyConfiguration(
        maxInitialLineLength = config.getInt(netty.MaxInitialLineLength),
        maxHeaderSize = config.getInt(netty.MaxHeaderSize),
        maxChunkSize = config.getInt(netty.MaxChunkSize),
        maxContentLength = config.getInt(netty.MaxContentLength)
      ),
      config
    )
  }
}

private[recorder] case class FiltersConfiguration(
    filterStrategy: FilterStrategy,
    whiteList:      WhiteList,
    blackList:      BlackList
) {

  def filters: Option[Filters] = filterStrategy match {
    case FilterStrategy.Disabled       => None
    case FilterStrategy.BlacklistFirst => Some(Filters(blackList, whiteList))
    case FilterStrategy.WhitelistFirst => Some(Filters(whiteList, blackList))
  }
}

private[recorder] case class CoreConfiguration(
  mode:                      RecorderMode,
  encoding:                  String,
  outputFolder:              String,
  bodiesFolder:              String,
  pkg:                       String,
  className:                 String,
  thresholdForPauseCreation: Duration,
  saveConfig:                Boolean,
  headless:                  Boolean,
  harFilePath:               Option[String]
)

private[recorder] case class HttpConfiguration(
  automaticReferer:    Boolean,
  followRedirect:      Boolean,
  inferHtmlResources:  Boolean,
  removeCacheHeaders:  Boolean,
  checkResponseBodies: Boolean
)

private[recorder] case class KeyStoreConfiguration(
  path:         String,
  password:     String,
  keyStoreType: KeyStoreType
)

private[recorder] case class CertificateAuthorityConfiguration(
  certificatePath: String,
  privateKeyPath:  String
)

private[recorder] case class HttpsModeConfiguration(
  mode:                 HttpsMode,
  keyStore:             KeyStoreConfiguration,
  certificateAuthority: CertificateAuthorityConfiguration
)

private[recorder] case class OutgoingProxyConfiguration(
  host:     Option[String],
  username: Option[String],
  password: Option[String],
  port:     Option[Int],
  sslPort:  Option[Int]
)

private[recorder] case class ProxyConfiguration(
  port:     Int,
  https:    HttpsModeConfiguration,
  outgoing: OutgoingProxyConfiguration
)

private[recorder] case class NettyConfiguration(
  maxInitialLineLength: Int,
  maxHeaderSize:        Int,
  maxChunkSize:         Int,
  maxContentLength:     Int
)

private[recorder] case class RecorderConfiguration(
  core:    CoreConfiguration,
  filters: FiltersConfiguration,
  http:    HttpConfiguration,
  proxy:   ProxyConfiguration,
  netty:   NettyConfiguration,
  config:  Config
)
