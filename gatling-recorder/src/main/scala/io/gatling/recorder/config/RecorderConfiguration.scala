/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.recorder.config

import java.io.{ File => JFile }

import scala.Array.canBuildFrom
import scala.collection.JavaConversions._
import scala.concurrent.duration.{ Duration, DurationInt }
import scala.reflect.io.Path.jfile2path
import scala.tools.nsc.io.File
import scala.util.Properties.userHome

import com.typesafe.config.{ Config, ConfigFactory, ConfigRenderOptions }
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import io.gatling.core.filter.{ BlackList, Filters, WhiteList }
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.core.util.StringHelper.{ RichString, eol }
import io.gatling.recorder.config.ConfigurationConstants._
import io.gatling.recorder.enumeration.FilterStrategy
import io.gatling.recorder.enumeration.FilterStrategy.FilterStrategy

object RecorderConfiguration extends StrictLogging {

	implicit class IntOption(val value: Int) extends AnyVal {
		def toOption = if (value != 0) Some(value) else None
	}

	val remove4Spaces = """\s{4}""".r

	val renderOptions = ConfigRenderOptions.concise.setFormatted(true).setJson(false)

	var configFile: Option[JFile] = None

	var configuration: RecorderConfiguration = _

	GatlingConfiguration.setUp()

	private[this] def getClassLoader() = Thread.currentThread.getContextClassLoader
	private[this] def getDefaultConfig(classLoader: ClassLoader) = ConfigFactory.parseResources(classLoader, "recorder-defaults.conf")

	def fakeConfig(props: Map[String, _]): RecorderConfiguration = {
		val defaultConfig = getDefaultConfig(getClassLoader)
		buildConfig(ConfigFactory.parseMap(props).withFallback(defaultConfig))
	}

	def initialSetup(props: Map[String, _]) {
		val classLoader = getClassLoader()
		val defaultConfig = getDefaultConfig(classLoader)
		configFile = Option(classLoader.getResource("recorder.conf")).map(url => new JFile(url.getFile))
		
		val customConfig = configFile.map(ConfigFactory.parseFile).getOrElse {
			// Should only happens with a manually (and incorrectly) updated Maven archetype or SBT template
			println("Maven archetype or SBT template outdated: Please create a new one or check the migration guide on how to update it.")
			println("Recorder preferences won't be saved until then.")
			ConfigFactory.empty
		}
		val propertiesConfig = ConfigFactory.parseMap(props)

		try {
			configuration = buildConfig(ConfigFactory.systemProperties
				.withFallback(propertiesConfig).withFallback(customConfig).withFallback(defaultConfig))
			logger.debug(s"configured $configuration")
		} catch {
			case e: Exception =>
				logger.warn(s"Loading configuration crashed: ${e.getMessage}. Probable cause is a format change, resetting.")
				configFile.foreach(_.delete)
				configuration = buildConfig(ConfigFactory.systemProperties.withFallback(propertiesConfig).withFallback(defaultConfig))
		}
	}

	def reload(props: Map[String, _]) {
		val frameConfig = ConfigFactory.parseMap(props)
		configuration = buildConfig(frameConfig.withFallback(configuration.config))
		logger.debug(s"reconfigured $configuration")
	}

	def saveConfig {
		// Removes first empty line and remove the extra level of indentation
		def cleanOutput(configToSave: String) = configToSave.split("\n").drop(1).map(remove4Spaces.replaceFirstIn(_, "")).mkString(eol)

		// Remove request bodies folder configuration (transient), keep only Gatling-related properties
		val configToSave = configuration.config.withoutPath(REQUEST_BODIES_FOLDER).root.withOnlyKey(CONFIG_ROOT)
		configFile.foreach(file => withCloseable(File(file).bufferedWriter)(_.write(cleanOutput(configToSave.render(renderOptions)))))
	}

	private def buildConfig(config: Config): RecorderConfiguration = {

		def getOutputFolder(folder: String) = {
			folder.trimToOption match {
				case Some(f) => f
				case _ if sys.env.contains("GATLING_HOME") => GatlingFiles.sourcesDirectory.toString
				case _ => userHome
			}
		}

		def getRequestBodiesFolder =
			if (config.hasPath(REQUEST_BODIES_FOLDER)) config.getString(REQUEST_BODIES_FOLDER)
			else GatlingFiles.requestBodiesDirectory.toString

		RecorderConfiguration(
			filters = FiltersConfiguration(
				filterStrategy = FilterStrategy.withName(config.getString(FILTER_STRATEGY)),
				whiteList = WhiteList(config.getStringList(WHITELIST_PATTERNS).toList),
				blackList = BlackList(config.getStringList(BLACKLIST_PATTERNS).toList)),
			http = HttpConfiguration(
				automaticReferer = config.getBoolean(AUTOMATIC_REFERER),
				followRedirect = config.getBoolean(FOLLOW_REDIRECT),
				fetchHtmlResources = config.getBoolean(FETCH_HTML_RESOURCES)),
			proxy = ProxyConfiguration(
				port = config.getInt(LOCAL_PORT),
				sslPort = config.getInt(LOCAL_SSL_PORT),
				outgoing = OutgoingProxyConfiguration(
					host = config.getString(PROXY_HOST).trimToOption,
					username = config.getString(PROXY_USERNAME).trimToOption,
					password = config.getString(PROXY_PASSWORD).trimToOption,
					port = config.getInt(PROXY_PORT).toOption,
					sslPort = config.getInt(PROXY_SSL_PORT).toOption)),
			core = CoreConfiguration(
				encoding = config.getString(ENCODING),
				outputFolder = getOutputFolder(config.getString(SIMULATION_OUTPUT_FOLDER)),
				requestBodiesFolder = getRequestBodiesFolder,
				pkg = config.getString(SIMULATION_PACKAGE),
				className = config.getString(SIMULATION_CLASS_NAME),
				thresholdForPauseCreation = config.getInt(THRESHOLD_FOR_PAUSE_CREATION) milliseconds),
			config)
	}
}

case class FiltersConfiguration(
	filterStrategy: FilterStrategy,
	whiteList: WhiteList,
	blackList: BlackList) {

	def filters: Option[Filters] = filterStrategy match {
		case FilterStrategy.DISABLED => None
		case FilterStrategy.BLACKLIST_FIRST => Some(Filters(blackList, whiteList))
		case FilterStrategy.WHITELIST_FIRST => Some(Filters(whiteList, blackList))
	}
}

case class HttpConfiguration(
	automaticReferer: Boolean,
	followRedirect: Boolean,
	fetchHtmlResources: Boolean)

case class OutgoingProxyConfiguration(
	host: Option[String],
	username: Option[String],
	password: Option[String],
	port: Option[Int],
	sslPort: Option[Int])

case class ProxyConfiguration(
	port: Int,
	sslPort: Int,
	outgoing: OutgoingProxyConfiguration)

case class CoreConfiguration(
	encoding: String,
	outputFolder: String,
	requestBodiesFolder: String,
	pkg: String,
	className: String,
	thresholdForPauseCreation: Duration)

case class RecorderConfiguration(
	filters: FiltersConfiguration,
	http: HttpConfiguration,
	proxy: ProxyConfiguration,
	core: CoreConfiguration,
	config: Config)