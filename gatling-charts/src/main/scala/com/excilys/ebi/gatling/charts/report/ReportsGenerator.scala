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

import java.io.FileOutputStream
import java.net.{ URL, URI, JarURLConnection }

import scala.collection.JavaConversions.asIterator
import scala.collection.mutable.LinkedHashSet
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.{ Path, File }

import com.excilys.ebi.gatling.charts.component.impl.ComponentLibraryImpl
import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.config.ChartsFiles.menuFile
import com.excilys.ebi.gatling.charts.loader.DataLoader
import com.excilys.ebi.gatling.charts.template.{ PageTemplate, MenuTemplate }
import com.excilys.ebi.gatling.charts.writer.TemplateWriter
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ styleFolder, jsFolder, GATLING_ASSETS_STYLE_PACKAGE, GATLING_ASSETS_JS_PACKAGE }
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.FileHelper.{ formatToFilename, HTML_EXTENSION }

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
		val dataLoader = new DataLoader(runOn)

		val reportGenerators =
			List(new ActiveSessionsReportGenerator(runOn, dataLoader, componentLibrary),
				new RequestsReportGenerator(runOn, dataLoader, componentLibrary),
				new RequestDetailsReportGenerator(runOn, dataLoader, componentLibrary))

		copyAssets(runOn)

		generateMenu(runOn, dataLoader)

		PageTemplate.setRunOn(dataLoader.simulationRunOn)

		reportGenerators.foreach(_.generate)
	}

	private def generateMenu(runOn: String, dataLoader: DataLoader) = {

		val maxLength = 50

		val requestLinks: Iterable[(String, Option[String], String)] = dataLoader.requestNames.map {
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
			destFolderPath.toDirectory.createDirectory()

			asIterator(getClass.getClassLoader.getResources(sourcePackage)).foreach { packageURL =>

				if (packageURL.getProtocol == "file") {
					new File(new java.io.File(new URI(packageURL.toString).getSchemeSpecificPart)).toDirectory.deepFiles.foreach { file =>
						file.copyTo(destFolderPath / file.name, true)
					}

				} else if (packageURL.getProtocol == "jar") {
					val jarCon = packageURL.openConnection.asInstanceOf[JarURLConnection]
					jarCon.setUseCaches(false)
					val jarEntryName = jarCon.getJarEntry.getName
					// JRockit predends with /, Hotspot doesn't
					val rootEntryPath = if (jarEntryName.endsWith("/")) jarEntryName else jarEntryName + "/"

					asIterator(jarCon.getJarFile.entries).foreach { entry =>
						val entryPath = entry.getName
						if (entryPath.startsWith(rootEntryPath) && entryPath != rootEntryPath) {
							val relativePath = entryPath.substring(rootEntryPath.length());
							val input = getClass.getClassLoader.getResourceAsStream(entryPath)
							val output = new FileOutputStream((destFolderPath / relativePath.replace('/', java.io.File.separatorChar)).jfile)

							try {
								val buffer = new Array[Byte](1024 * 4)
								var n = input.read(buffer)
								while (n != -1) {
									output.write(buffer, 0, n);
									n = input.read(buffer)
								}
							} finally {
								output.close
							}
						}
					}
				}
			}
		}

		copyFolder(GATLING_ASSETS_STYLE_PACKAGE, styleFolder(runOn))
		copyFolder(GATLING_ASSETS_JS_PACKAGE, jsFolder(runOn))
	}
}