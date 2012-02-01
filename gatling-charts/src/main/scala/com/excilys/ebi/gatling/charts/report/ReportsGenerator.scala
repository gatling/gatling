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
package com.excilys.ebi.gatling.charts.report

import java.io.{ File => JFile }
import java.net.{ URL, URI }

import scala.collection.JavaConversions.asIterator
import scala.collection.mutable.LinkedHashSet
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.{ Path, Jar, File }

import com.excilys.ebi.gatling.charts.component.impl.ComponentLibraryImpl
import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.config.ChartsFiles.menuFile
import com.excilys.ebi.gatling.charts.template.{ PageTemplate, MenuTemplate }
import com.excilys.ebi.gatling.charts.writer.TemplateWriter
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ styleFolder, jsFolder, GATLING_ASSETS_STYLE_PACKAGE, GATLING_ASSETS_JS_PACKAGE }
import com.excilys.ebi.gatling.core.config.GatlingConfig
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.FileHelper.{ formatToFilename, HTML_EXTENSION }
import com.excilys.ebi.gatling.core.util.IOHelper

object ReportsGenerator extends Logging {

	val STATIC_LIBRARY_BINDER_PATH = "com/excilys/ebi/gatling/charts/component/impl/ComponentLibraryImpl.class"

	val componentLibrary: ComponentLibrary = {
		val reportsGeneratorClassLoader = this.getClass.getClassLoader
		val paths = if (reportsGeneratorClassLoader == null) {
			ClassLoader.getSystemResources(STATIC_LIBRARY_BINDER_PATH)
		} else {
			reportsGeneratorClassLoader.getResources(STATIC_LIBRARY_BINDER_PATH)
		}
		// LinkedHashSet appropriate here because it preserves insertion order during iteration
		val implementationSet = new LinkedHashSet[URL]
		while (paths.hasMoreElements) {
			val path = paths.nextElement.asInstanceOf[URL]
			implementationSet += path
		}
		if (implementationSet.size > 1) {
			logger.warn("Class path contains multiple ComponentLibrary bindings")
			implementationSet.foreach(logger.warn("Found ComponentLibrary binding in {}", _))
		}

		new ComponentLibraryImpl
	}

	def generateFor(runOn: String) = {
		val dataReader = DataReader.newInstance(runOn)

		if (dataReader.requestNames.isEmpty) {
			logger.warn("There were no requests sent during the simulation, reports won't be generated")
			false

		} else {
			val reportGenerators =
				List(new ActiveSessionsReportGenerator(runOn, dataReader, componentLibrary),
					new RequestsReportGenerator(runOn, dataReader, componentLibrary),
					new TransactionsReportGenerator(runOn, dataReader, componentLibrary),
					new RequestDetailsReportGenerator(runOn, dataReader, componentLibrary))

			copyAssets(runOn)

			generateMenu(runOn, dataReader)

			PageTemplate.setRunOn(dataReader.simulationRunOn)

			reportGenerators.foreach(_.generate)

			true
		}
	}

	private def generateMenu(runOn: String, dataReader: DataReader) = {

		val maxLength = 50

		val requestLinks: Iterable[(String, Option[String], String)] = dataReader.requestNames.map {
			requestName =>
				val title = if (requestName.length > maxLength) Some(requestName.substring(8)) else None
				val printedName = if (requestName.length > maxLength) requestName.substring(8, maxLength) + "..." else requestName.substring(8)
				(formatToFilename(requestName) + HTML_EXTENSION, title, printedName)
		}

		val template = new MenuTemplate(requestLinks)

		new TemplateWriter(menuFile(runOn)).writeToFile(template.getOutput)
	}

	private def copyAssets(runOn: String) = {
		def copyFolder(sourcePackage: String, destFolderPath: Path) = {

			for (packageURL <- asIterator(getClass.getClassLoader.getResources(sourcePackage))) {
				packageURL.getProtocol match {
					case "file" =>
						for (file <- new File(new JFile(new URI(packageURL.toString).getSchemeSpecificPart)).toDirectory.deepFiles) {
							val target = destFolderPath / file.name
							target.parent.createDirectory()
							file.copyTo(target, true)
						}

					case "jar" =>
						val jarFilePath = packageURL.getPath.substring(0, packageURL.getPath.indexOf('!'))
						val rootEntryPath = if (sourcePackage.endsWith("/")) sourcePackage else sourcePackage + "/"

						for (fileish <- new Jar(new File(new JFile(new URI(jarFilePath)))).fileishIterator.filter(_.parent.toString == sourcePackage)) {
							val target = destFolderPath / fileish.name
							target.parent.createDirectory()
							val input = fileish.input()
							val output = target.toFile.outputStream(false)
							IOHelper.copy(input, output)
						}
					case _ => throw new UnsupportedOperationException
				}
			}
		}

		copyFolder(GATLING_ASSETS_STYLE_PACKAGE, styleFolder(runOn))
		copyFolder(GATLING_ASSETS_JS_PACKAGE, jsFolder(runOn))
	}
}