/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
import java.nio.charset.Charset
import java.nio.file.{ Files, Path, Paths }

import scala.concurrent.duration.{ Duration, DurationInt }
import scala.jdk.CollectionConverters._
import scala.util.Using
import scala.util.control.NonFatal

import io.gatling.commons.util.ConfigHelper._
import io.gatling.commons.util.StringHelper.RichString
import io.gatling.commons.util.Throwables._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.filter.{ AllowList, DenyList, Filters }
import io.gatling.recorder.cli.RecorderArgs
import io.gatling.recorder.http.ssl.{ HttpsMode, KeyStoreType }
import io.gatling.recorder.render.template.RenderingFormat

import com.typesafe.config.{ Config, ConfigFactory, ConfigRenderOptions }
import com.typesafe.scalalogging.StrictLogging

private[recorder] object RecorderConfiguration extends StrictLogging {
  implicit class IntOption(val value: Int) extends AnyVal {
    def toOption: Option[Int] = if (value != 0) Some(value) else None
  }

  private val RenderOptions = ConfigRenderOptions.concise.setFormatted(true).setJson(false)

  private var configFile: Option[Path] = None

  private var _configuration: Option[RecorderConfiguration] = None
  def recorderConfiguration: RecorderConfiguration =
    _configuration.getOrElse(throw new UnsupportedOperationException("RecorderConfiguration hasn't been loaded yet"))

  private[this] val gatlingConfiguration: GatlingConfiguration = GatlingConfiguration.load()

  private[this] def getClassLoader = Thread.currentThread.getContextClassLoader
  private[this] def getDefaultConfig(classLoader: ClassLoader) =
    ConfigFactory.parseResources(classLoader, "recorder-defaults.conf")

  def testConfig(args: RecorderArgs, fakeSystemProps: Map[String, _ <: Any]): RecorderConfiguration = {
    val defaultConfig = getDefaultConfig(getClassLoader)
    buildConfig(args, configChain(ConfigFactory.parseMap(fakeSystemProps.asJava), defaultConfig))
  }

  def initialSetup(args: RecorderArgs): Unit = {
    val classLoader = getClassLoader
    val defaultConfig = getDefaultConfig(classLoader)

    val userConfigFile = args.resourcesFolder.resolve("recorder.conf")
    configFile = Some(userConfigFile)

    val customConfig =
      if (Files.exists(userConfigFile)) {
        println("Loading preferences from existing recorder.conf")
        ConfigFactory.parseFile(userConfigFile.toFile)
      } else {
        ConfigFactory.empty
      }

    try {
      _configuration = Some(buildConfig(args, configChain(ConfigFactory.systemProperties, customConfig, defaultConfig)))
    } catch {
      case NonFatal(e) =>
        logger.warn(s"Loading configuration crashed: ${e.rootMessage}. Probable cause is a format change, resetting.")
        configFile.foreach { file =>
          if (Files.exists(file)) {
            Files.delete(file)
          }
        }
        _configuration = Some(buildConfig(args, configChain(ConfigFactory.systemProperties, defaultConfig)))
    }
  }

  def reload(newRecorderConfiguration: RecorderConfiguration): Unit =
    _configuration = Some(newRecorderConfiguration)

  def saveConfig(): Unit =
    configFile.foreach { file =>
      val configMap = Map(
        ConfigKeys.core.Mode -> recorderConfiguration.core.mode.toString,
        ConfigKeys.core.Encoding -> recorderConfiguration.core.encoding.name,
        ConfigKeys.core.Package -> recorderConfiguration.core.pkg,
        ConfigKeys.core.ClassName -> recorderConfiguration.core.className,
        ConfigKeys.core.ThresholdForPauseCreation -> recorderConfiguration.core.thresholdForPauseCreation.toMillis,
        ConfigKeys.core.SaveConfig -> recorderConfiguration.core.saveConfig,
        ConfigKeys.core.Headless -> recorderConfiguration.core.headless,
        ConfigKeys.core.HarFilePath -> recorderConfiguration.core.harFilePath.getOrElse(""),
        ConfigKeys.core.Format -> recorderConfiguration.core.format.toString,
        ConfigKeys.filters.Enable -> recorderConfiguration.filters.enabled,
        ConfigKeys.filters.AllowListPatterns -> recorderConfiguration.filters.allowList.patterns.asJava,
        ConfigKeys.filters.DenyListPatterns -> recorderConfiguration.filters.denyList.patterns.asJava,
        ConfigKeys.http.AutomaticReferer -> recorderConfiguration.http.automaticReferer,
        ConfigKeys.http.FollowRedirect -> recorderConfiguration.http.followRedirect,
        ConfigKeys.http.InferHtmlResources -> recorderConfiguration.http.inferHtmlResources,
        ConfigKeys.http.RemoveCacheHeaders -> recorderConfiguration.http.removeCacheHeaders,
        ConfigKeys.http.CheckResponseBodies -> recorderConfiguration.http.checkResponseBodies,
        ConfigKeys.http.UseSimulationAsPrefix -> recorderConfiguration.http.useSimulationAsPrefix,
        ConfigKeys.http.UseMethodAndUriAsPostfix -> recorderConfiguration.http.useMethodAndUriAsPostfix,
        ConfigKeys.proxy.Port -> recorderConfiguration.proxy.port,
        ConfigKeys.proxy.https.Mode -> recorderConfiguration.proxy.https.mode.toString
      ) ++ (
        recorderConfiguration.proxy.https.mode match {
          case HttpsMode.ProvidedKeyStore =>
            Map(
              ConfigKeys.proxy.https.keyStore.Type -> recorderConfiguration.proxy.https.keyStore.keyStoreType.toString,
              ConfigKeys.proxy.https.keyStore.Path -> recorderConfiguration.proxy.https.keyStore.path.toString,
              ConfigKeys.proxy.https.keyStore.Password -> recorderConfiguration.proxy.https.keyStore.password
            )
          case HttpsMode.CertificateAuthority =>
            Map(
              ConfigKeys.proxy.https.certificateAuthority.CertificatePath -> recorderConfiguration.proxy.https.certificateAuthority.certificatePath.toString,
              ConfigKeys.proxy.https.certificateAuthority.PrivateKeyPath -> recorderConfiguration.proxy.https.certificateAuthority.privateKeyPath.toString
            )
          case _ =>
            Map.empty
        }
      )

      val configToSave = ConfigFactory.parseMap(configMap.asJava).root.withOnlyKey(ConfigKeys.ConfigRoot)

      Using.resource(Files.newBufferedWriter(createAndOpen(file), gatlingConfiguration.core.charset))(_.write(configToSave.render(RenderOptions)))
    }

  private[config] def createAndOpen(path: Path): Path =
    if (!Files.exists(path)) {
      val parent = path.getParent
      if (!Files.exists(parent)) {
        throw new FileNotFoundException(s"Directory '${parent.toString}' for recorder configuration does not exist")
      }
      Files.createFile(path)
    } else {
      path
    }

  private def buildConfig(args: RecorderArgs, config: Config): RecorderConfiguration = {
    import ConfigKeys._

    RecorderConfiguration(
      core = CoreConfiguration(
        simulationsFolder = args.simulationsFolder,
        resourcesFolder = args.resourcesFolder,
        mode = RecorderMode(config.getString(core.Mode)),
        encoding = Charset.forName(config.getString(core.Encoding)),
        pkg = args.pkg.getOrElse(config.getString(core.Package)),
        className = args.className.getOrElse(config.getString(core.ClassName)),
        thresholdForPauseCreation = config.getInt(core.ThresholdForPauseCreation).milliseconds,
        saveConfig = config.getBoolean(core.SaveConfig),
        headless = config.getBoolean(core.Headless),
        harFilePath = config.getString(core.HarFilePath).trimToOption.map(p => Path.of(p)),
        format = args.format
          .orElse(
            config
              .getStringOption(core.Format)
              .map(RenderingFormat.fromString)
          )
          .getOrElse(RenderingFormat.defaultFromJvm)
      ),
      filters = FiltersConfiguration(
        enabled = config.getBoolean(filters.Enable),
        allowList = new AllowList(config.getStringList(filters.AllowListPatterns).asScala.toList),
        denyList = new DenyList(config.getStringList(filters.DenyListPatterns).asScala.toList)
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
            path = Paths.get(config.getString(proxy.https.keyStore.Path)),
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
      )
    )
  }
}

private[recorder] final case class FiltersConfiguration(
    enabled: Boolean,
    allowList: AllowList,
    denyList: DenyList
) {
  def filters: Option[Filters] =
    if (enabled) {
      Some(new Filters(denyList, allowList))
    } else {
      None
    }
}

private[recorder] final case class CoreConfiguration(
    simulationsFolder: Path,
    resourcesFolder: Path,
    mode: RecorderMode,
    encoding: Charset,
    pkg: String,
    className: String,
    thresholdForPauseCreation: Duration,
    saveConfig: Boolean,
    headless: Boolean,
    harFilePath: Option[Path],
    format: RenderingFormat
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
    path: Path,
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

private[recorder] final case class RecorderConfiguration(
    core: CoreConfiguration,
    filters: FiltersConfiguration,
    http: HttpConfiguration,
    proxy: ProxyConfiguration
)
