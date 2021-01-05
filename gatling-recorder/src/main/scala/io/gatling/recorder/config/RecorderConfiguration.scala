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

package io.gatling.recorder.config

import java.io.FileNotFoundException
import java.nio.file.{ Path, Paths }

import scala.collection.mutable
import scala.concurrent.duration.{ Duration, DurationInt }
import scala.jdk.CollectionConverters._
import scala.util.Properties.userHome
import scala.util.Using
import scala.util.control.NonFatal

import io.gatling.commons.shared.unstable.util.PathHelper._
import io.gatling.commons.util.ConfigHelper.configChain
import io.gatling.commons.util.StringHelper.RichString
import io.gatling.commons.util.Throwables._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingFiles._
import io.gatling.core.filter.{ BlackList, Filters, WhiteList }
import io.gatling.recorder.http.ssl.{ HttpsMode, KeyStoreType }

import com.typesafe.config.{ Config, ConfigFactory, ConfigRenderOptions }
import com.typesafe.scalalogging.StrictLogging

private[recorder] object RecorderConfiguration extends StrictLogging {

  implicit class IntOption(val value: Int) extends AnyVal {
    def toOption: Option[Int] = if (value != 0) Some(value) else None
  }

  private val RenderOptions = ConfigRenderOptions.concise.setFormatted(true).setJson(false)

  var configFile: Option[Path] = None

  implicit var configuration: RecorderConfiguration = _

  private[this] val gatlingConfiguration: GatlingConfiguration = GatlingConfiguration.load(mutable.Map.empty)

  private[this] def getClassLoader = Thread.currentThread.getContextClassLoader
  private[this] def getDefaultConfig(classLoader: ClassLoader) =
    ConfigFactory.parseResources(classLoader, "recorder-defaults.conf")

  def fakeConfig(props: mutable.Map[String, _ <: Any]): RecorderConfiguration = {
    val defaultConfig = getDefaultConfig(getClassLoader)
    buildConfig(configChain(ConfigFactory.parseMap(props.asJava), defaultConfig))
  }

  def initialSetup(props: mutable.Map[String, _ <: Any], recorderConfigFile: Option[Path]): Unit = {
    val classLoader = getClassLoader
    val defaultConfig = getDefaultConfig(classLoader)
    configFile = recorderConfigFile.orElse(Option(classLoader.getResource("recorder.conf")).map(url => Paths.get(url.toURI)))

    val customConfig = configFile.map(path => ConfigFactory.parseFile(path.toFile)).getOrElse {
      // Should only happens with a manually (and incorrectly) updated Maven archetype or SBT template
      println("recorder.conf file couldn't be located or is outdated")
      println("Recorder preferences won't be saved.")
      println("""If running from sbt, please run "copyConfigFiles" and check the plugin documentation.""")
      ConfigFactory.empty
    }
    val propertiesConfig = ConfigFactory.parseMap(props.asJava)

    try {
      configuration = buildConfig(configChain(ConfigFactory.systemProperties, propertiesConfig, customConfig, defaultConfig))
    } catch {
      case NonFatal(e) =>
        logger.warn(s"Loading configuration crashed: ${e.rootMessage}. Probable cause is a format change, resetting.")
        configFile.foreach(_.delete())
        configuration = buildConfig(configChain(ConfigFactory.systemProperties, propertiesConfig, defaultConfig))
    }
  }

  def reload(props: mutable.Map[String, _ <: Any]): Unit = {
    val frameConfig = ConfigFactory.parseMap(props.asJava)
    configuration = buildConfig(configChain(frameConfig, configuration.config))
  }

  def saveConfig(): Unit = {
    // Remove request bodies folder configuration (transient), keep only Gatling-related properties
    val configToSave = configuration.config.withoutPath(ConfigKeys.core.ResourcesFolder).root.withOnlyKey(ConfigKeys.ConfigRoot)
    configFile.foreach(file => Using.resource(createAndOpen(file).writer(gatlingConfiguration.core.charset))(_.write(configToSave.render(RenderOptions))))
  }

  private[config] def createAndOpen(path: Path): Path =
    if (!path.exists) {
      val parent = path.getParent
      if (!parent.exists) {
        throw new FileNotFoundException(s"Directory '${parent.toString}' for recorder configuration does not exist")
      }
      path.createFile()
    } else {
      path
    }

  private def buildConfig(config: Config): RecorderConfiguration = {
    import ConfigKeys._

    def getSimulationsFolder(folder: String) =
      folder.trimToOption match {
        case Some(f)                               => f
        case _ if sys.env.contains("GATLING_HOME") => simulationsDirectory(gatlingConfiguration).toFile.toString
        case _                                     => userHome
      }

    def getResourcesFolder =
      if (config.hasPath(core.ResourcesFolder)) config.getString(core.ResourcesFolder)
      else resourcesDirectory(gatlingConfiguration).toFile.toString

    RecorderConfiguration(
      core = CoreConfiguration(
        mode = RecorderMode(config.getString(core.Mode)),
        encoding = config.getString(core.Encoding),
        simulationsFolder = getSimulationsFolder(config.getString(core.SimulationsFolder)),
        resourcesFolder = getResourcesFolder,
        pkg = config.getString(core.Package),
        className = config.getString(core.ClassName),
        thresholdForPauseCreation = config.getInt(core.ThresholdForPauseCreation).milliseconds,
        saveConfig = config.getBoolean(core.SaveConfig),
        headless = config.getBoolean(core.Headless),
        harFilePath = config.getString(core.HarFilePath).trimToOption
      ),
      filters = FiltersConfiguration(
        filterStrategy = FilterStrategy(config.getString(filters.FilterStrategy)),
        whiteList = new WhiteList(config.getStringList(filters.WhitelistPatterns).asScala.toList),
        blackList = new BlackList(config.getStringList(filters.BlacklistPatterns).asScala.toList)
      ),
      http = HttpConfiguration(
        automaticReferer = config.getBoolean(http.AutomaticReferer),
        followRedirect = config.getBoolean(http.FollowRedirect),
        inferHtmlResources = config.getBoolean(http.InferHtmlResources),
        removeCacheHeaders = config.getBoolean(http.RemoveCacheHeaders),
        checkResponseBodies = config.getBoolean(http.CheckResponseBodies),
        useSimulationAsPrefix = config.getBoolean(http.UseSimulationAsPrefix),
        useMethodAndUriAsPostfix = config.getBoolean(http.UseMethodAndUriAsPostfix)
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
            certificatePath = Paths.get(config.getString(proxy.https.certificateAuthority.CertificatePath)),
            privateKeyPath = Paths.get(config.getString(proxy.https.certificateAuthority.PrivateKeyPath))
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

private[recorder] final case class FiltersConfiguration(
    filterStrategy: FilterStrategy,
    whiteList: WhiteList,
    blackList: BlackList
) {

  def filters: Option[Filters] = filterStrategy match {
    case FilterStrategy.Disabled       => None
    case FilterStrategy.BlackListFirst => Some(new Filters(blackList, whiteList))
    case FilterStrategy.WhiteListFirst => Some(new Filters(whiteList, blackList))
  }
}

private[recorder] final case class CoreConfiguration(
    mode: RecorderMode,
    encoding: String,
    simulationsFolder: String,
    resourcesFolder: String,
    pkg: String,
    className: String,
    thresholdForPauseCreation: Duration,
    saveConfig: Boolean,
    headless: Boolean,
    harFilePath: Option[String]
)

private[recorder] final case class HttpConfiguration(
    automaticReferer: Boolean,
    followRedirect: Boolean,
    inferHtmlResources: Boolean,
    removeCacheHeaders: Boolean,
    checkResponseBodies: Boolean,
    useSimulationAsPrefix: Boolean,
    useMethodAndUriAsPostfix: Boolean
)

private[recorder] final case class KeyStoreConfiguration(
    path: String,
    password: String,
    keyStoreType: KeyStoreType
)

private[recorder] final case class CertificateAuthorityConfiguration(
    certificatePath: Path,
    privateKeyPath: Path
)

private[recorder] final case class HttpsModeConfiguration(
    mode: HttpsMode,
    keyStore: KeyStoreConfiguration,
    certificateAuthority: CertificateAuthorityConfiguration
)

private[recorder] final case class OutgoingProxyConfiguration(
    host: Option[String],
    username: Option[String],
    password: Option[String],
    port: Option[Int],
    sslPort: Option[Int]
)

private[recorder] final case class ProxyConfiguration(
    port: Int,
    https: HttpsModeConfiguration,
    outgoing: OutgoingProxyConfiguration
)

private[recorder] final case class NettyConfiguration(
    maxInitialLineLength: Int,
    maxHeaderSize: Int,
    maxChunkSize: Int,
    maxContentLength: Int
)

private[recorder] final case class RecorderConfiguration(
    core: CoreConfiguration,
    filters: FiltersConfiguration,
    http: HttpConfiguration,
    proxy: ProxyConfiguration,
    netty: NettyConfiguration,
    config: Config
)
