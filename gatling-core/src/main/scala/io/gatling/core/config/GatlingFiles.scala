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
package io.gatling.core.config

import scala.collection.mutable

import scala.tools.nsc.io.{ Directory, Path }
import scala.tools.nsc.io.Path.string2path

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

object GatlingFiles {

	val GATLING_HOME = Option(System.getenv("GATLING_HOME")).getOrElse(".")
	val GATLING_ASSETS_PACKAGE = "assets"
	val GATLING_JS = "js"
	val GATLING_STYLE = "style"
	val GATLING_ASSETS_JS_PACKAGE = GATLING_ASSETS_PACKAGE / GATLING_JS
	val GATLING_ASSETS_STYLE_PACKAGE = GATLING_ASSETS_PACKAGE / GATLING_STYLE
	private val requestBodyFileMemo = mutable.Map.empty[Path, Validation[Resource]]
	private val feederFileMemo = mutable.Map.empty[Path, Validation[Resource]]

	private def resolvePath(path: String): Path = {
		val rawPath = Path(path)
		if (rawPath.isAbsolute) path else GATLING_HOME / path
	}

	def dataDirectory: Path = resolvePath(configuration.core.directory.data)
	def resultsRootDirectory: Path = resolvePath(configuration.core.directory.results)
	def requestBodiesDirectory: Path = resolvePath(configuration.core.directory.requestBodies)
	def sourcesDirectory: Directory = resolvePath(configuration.core.directory.sources).toDirectory
	def reportsOnlyDirectory: Option[String] = configuration.core.directory.reportsOnly
	def binariesDirectory: Option[Directory] = configuration.core.directory.binaries.map(_.toDirectory)
	def resultDirectory(runUuid: String): Path = resultsRootDirectory / runUuid
	def jsDirectory(runUuid: String): Path = resultDirectory(runUuid) / GATLING_JS
	def styleDirectory(runUuid: String): Path = resultDirectory(runUuid) / GATLING_STYLE

	def simulationLogDirectory(runUuid: String, create: Boolean = true): Directory = {
		val dir = resultDirectory(runUuid)
		if (create)
			dir.createDirectory()
		else {
			require(dir.exists, s"simulation directory '${dir.toAbsolute}' doesn't exist")
			require(dir.isDirectory, s"simulation directory '${dir.toAbsolute}' is not a directory")

			dir.toDirectory
		}
	}

	def validateResource(filePath: Path, defaultFolder: String): Validation[Resource] = {
		val defaultPath = defaultFolder / filePath
		val classPathResource = Option(getClass.getClassLoader.getResourceAsStream((defaultPath).toString))
			.map(is => ClassPathResource(is, filePath.extension))
		val resource = classPathResource.orElse(filePath.ifFile(path => FileResource(path.toFile)))
		resource.map(_.success).getOrElse(s"file $filePath doesn't exist".failure)
	}

	def requestBodyResource(filePath: Path) = requestBodyFileMemo.getOrElseUpdate(filePath, validateResource(filePath, "request-bodies"))

	def feederResource(filePath: Path) = feederFileMemo.getOrElseUpdate(filePath, validateResource(filePath, "data"))
}