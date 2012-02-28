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

import java.net.URL
import scala.collection.mutable.LinkedHashSet
import com.excilys.ebi.gatling.charts.component.impl.ComponentLibraryImpl
import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.config.ChartsFiles.menuFile
import com.excilys.ebi.gatling.charts.template.{ PageTemplate, MenuTemplate }
import com.excilys.ebi.gatling.charts.writer.TemplateWriter
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ styleFolder, jsFolder, GATLING_ASSETS_STYLE_PACKAGE, GATLING_ASSETS_JS_PACKAGE }
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.FileHelper.{ formatToFilename, HTML_EXTENSION }
import com.excilys.ebi.gatling.core.util.ScanHelper.deepCopyPackageContent
import grizzled.slf4j.Logging

object ReportsGenerator extends Logging {

	val STATIC_LIBRARY_BINDER_PATH = "com/excilys/ebi/gatling/charts/component/impl/ComponentLibraryImpl.class"

	val componentLibrary: ComponentLibrary = {
		val paths = Option(this.getClass.getClassLoader) match {
			case Some(classloader) => classloader.getResources(STATIC_LIBRARY_BINDER_PATH)
			case None => ClassLoader.getSystemResources(STATIC_LIBRARY_BINDER_PATH)
		}

		// LinkedHashSet appropriate here because it preserves insertion order during iteration
		val implementationSet = new LinkedHashSet[URL]
		while (paths.hasMoreElements) {
			val path = paths.nextElement.asInstanceOf[URL]
			implementationSet += path
		}
		if (implementationSet.size > 1) {
			warn("Class path contains multiple ComponentLibrary bindings")
			implementationSet.foreach(url => warn("Found ComponentLibrary binding in " + url))
		}

		new ComponentLibraryImpl
	}

	def generateFor(runUuid: String) = {
		val dataReader = DataReader.newInstance(runUuid)

		if (dataReader.requestNames.isEmpty) {
			warn("There were no requests sent during the simulation, reports won't be generated")
			false

		} else {
			val reportGenerators =
				List(new ActiveSessionsReportGenerator(runUuid, dataReader, componentLibrary),
					new RequestsReportGenerator(runUuid, dataReader, componentLibrary),
					new TransactionsReportGenerator(runUuid, dataReader, componentLibrary),
					new RequestDetailsReportGenerator(runUuid, dataReader, componentLibrary))

			copyAssets(runUuid)

			generateMenu(runUuid, dataReader)

			PageTemplate.setRunInfo(dataReader.runRecord)

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
		deepCopyPackageContent(GATLING_ASSETS_STYLE_PACKAGE, styleFolder(runOn))
		deepCopyPackageContent(GATLING_ASSETS_JS_PACKAGE, jsFolder(runOn))
	}
}