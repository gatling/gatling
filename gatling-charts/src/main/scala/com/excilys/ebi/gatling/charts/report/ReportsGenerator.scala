/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.report
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Path
import org.fusesource.scalate.support.ScalaCompiler
import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.config.ChartsFiles.menuFile
import com.excilys.ebi.gatling.charts.loader.DataLoader
import com.excilys.ebi.gatling.charts.template.{ PageTemplate, MenuTemplate }
import com.excilys.ebi.gatling.charts.writer.TemplateWriter
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_CHARTING_COMPONENT_LIBRARY_CLASS
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ styleFolder, jsFolder, GATLING_ASSETS_STYLE_FOLDER, GATLING_ASSETS_JS_FOLDER }
import com.excilys.ebi.gatling.core.util.FileHelper.{ formatToFilename, HTML_EXTENSION }
import com.excilys.ebi.gatling.core.util.ReflectionHelper.getNewInstanceByClassName
import com.excilys.ebi.gatling.core.log.Logging
import scala.collection.mutable.MutableList

object ReportsGenerator extends Logging {
	def generateFor(runOn: String) = {
		val dataLoader = new DataLoader(runOn)

		val componentLibrary = getNewInstanceByClassName[ComponentLibrary](CONFIG_CHARTING_COMPONENT_LIBRARY_CLASS)

		val reportGenerators =
			List(new ActiveSessionsReportGenerator(runOn, dataLoader, componentLibrary),
				new RequestsReportGenerator(runOn, dataLoader, componentLibrary),
				new RequestDetailsReportGenerator(runOn, dataLoader, componentLibrary))

		copyAssets(runOn)

		generateMenu(runOn, dataLoader)

		reportGenerators.foreach(_.generate)

		PageTemplate.TEMPLATE_ENGINE.compiler.asInstanceOf[ScalaCompiler].compiler.askShutdown
	}

	private def generateMenu(runOn: String, dataLoader: DataLoader) = {

		val requestLinks: Iterable[(String, Option[String], String)] = dataLoader.requestNames.map {
			requestName =>
				val title = if (requestName.length > 36) Some(requestName.substring(8)) else None
				val printedName = if (requestName.length > 36) requestName.substring(8, 36) + "..." else requestName.substring(8)
				(formatToFilename(requestName) + HTML_EXTENSION, title, printedName)
		}

		val template = new MenuTemplate(requestLinks)

		new TemplateWriter(menuFile(runOn)).writeToFile(template.getOutput)
	}

	private def copyAssets(runOn: String) = {
		def copyFolder(sourceFolderName: Path, destFolderPath: Path) = {
			destFolderPath.toDirectory.createDirectory()

			sourceFolderName.toDirectory.deepFiles.foreach { file =>
				file.copyTo(destFolderPath / file.name, true)
			}
		}

		copyFolder(GATLING_ASSETS_STYLE_FOLDER, styleFolder(runOn))
		copyFolder(GATLING_ASSETS_JS_FOLDER, jsFolder(runOn))
	}
}