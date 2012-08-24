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

import scala.tools.nsc.io.Path

import com.excilys.ebi.gatling.charts.component.{ ComponentLibrary, RequestStatistics }
import com.excilys.ebi.gatling.charts.config.ChartsFiles.{ globalFile, menuFile }
import com.excilys.ebi.gatling.charts.template.{ MenuTemplate, PageTemplate }
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ GATLING_ASSETS_JS_PACKAGE, GATLING_ASSETS_STYLE_PACKAGE, jsDirectory, styleDirectory }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.FileHelper.{ HTML_EXTENSION, formatToFilename }
import com.excilys.ebi.gatling.core.util.ScanHelper.deepCopyPackageContent

import grizzled.slf4j.Logging

object ReportsGenerator extends Logging {

	def generateFor(runUuid: String): Path = {

		val dataReader = DataReader.newInstance(runUuid)

		def generateMenu {
			val maxLength = 50

			val requestLinks: Iterable[(String, Option[String], String)] = dataReader.requestNames.map {
				requestName =>
					val title = if (requestName.length > maxLength) Some(requestName) else None
					val printedName = if (requestName.length > maxLength) requestName.substring(maxLength) + "..." else requestName
					(formatToFilename(requestName) + HTML_EXTENSION, title, printedName)
			}

			val template = new MenuTemplate(requestLinks)

			new TemplateWriter(menuFile(runUuid)).writeToFile(template.getOutput)
		}

		def generateStats: Map[String, RequestStatistics] = new StatsReportGenerator(runUuid, dataReader, ComponentLibrary.instance).generate

		def copyAssets {
			deepCopyPackageContent(GATLING_ASSETS_STYLE_PACKAGE, styleDirectory(runUuid))
			deepCopyPackageContent(GATLING_ASSETS_JS_PACKAGE, jsDirectory(runUuid))
		}

		if (dataReader.requestNames.isEmpty) {
			throw new UnsupportedOperationException("There were no requests sent during the simulation, reports won't be generated")

		} else {
			val reportGenerators =
				List(new AllSessionsReportGenerator(runUuid, dataReader, ComponentLibrary.instance),
					new GlobalReportGenerator(runUuid, dataReader, ComponentLibrary.instance),
					new RequestDetailsReportGenerator(runUuid, dataReader, ComponentLibrary.instance))

			copyAssets
			generateMenu
			PageTemplate.setRunInfo(dataReader.runRecord)
			reportGenerators.foreach(_.generate)
			generateStats

			globalFile(runUuid)
		}
	}
}
