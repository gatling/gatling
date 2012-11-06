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
import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.util.FileHelper.requestFileName
import com.excilys.ebi.gatling.core.util.ScanHelper.deepCopyPackageContent
import com.excilys.ebi.gatling.core.util.StringHelper.escapeJsQuoteString

import grizzled.slf4j.Logging

object ReportsGenerator extends Logging {

	def generateFor(outputDirectoryName: String): Path = {

		val dataReader = DataReader.newInstance(outputDirectoryName)

		def generateMenu {
			val maxLength = 50

			val requestLinks: Iterable[(String, Option[String], String)] = dataReader.requestNames.map {
				requestName =>
					val fileName = requestFileName(requestName)
					val title = if (requestName.length > maxLength) Some(escapeJsQuoteString(requestName)) else None
					val printedName = escapeJsQuoteString(if (requestName.length > maxLength) requestName.substring(maxLength) + "..." else requestName)

					(fileName, title, printedName)
			}

			val template = new MenuTemplate(requestLinks)

			new TemplateWriter(menuFile(outputDirectoryName)).writeToFile(template.getOutput)
		}

		def generateStats: Map[String, RequestStatistics] = new StatsReportGenerator(outputDirectoryName, dataReader, ComponentLibrary.instance).generate

		def copyAssets {
			deepCopyPackageContent(GATLING_ASSETS_STYLE_PACKAGE, styleDirectory(outputDirectoryName))
			deepCopyPackageContent(GATLING_ASSETS_JS_PACKAGE, jsDirectory(outputDirectoryName))
		}

		if (dataReader.requestNames.isEmpty) throw new UnsupportedOperationException("There were no requests sent during the simulation, reports won't be generated")

		val reportGenerators =
			List(new AllSessionsReportGenerator(outputDirectoryName, dataReader, ComponentLibrary.instance),
				new GlobalReportGenerator(outputDirectoryName, dataReader, ComponentLibrary.instance),
				new RequestDetailsReportGenerator(outputDirectoryName, dataReader, ComponentLibrary.instance))

		copyAssets
		generateMenu
		PageTemplate.setRunInfo(dataReader.runRecord,dataReader.runStart,dataReader.runEnd)
		reportGenerators.foreach(_.generate)
		generateStats

		globalFile(outputDirectoryName)
	}
}