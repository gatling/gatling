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
import scala.tools.nsc.io.File
import com.excilys.ebi.gatling.charts.component.ComponentLibrary
import com.excilys.ebi.gatling.charts.loader.DataLoader
import com.excilys.ebi.gatling.charts.template.MenuTemplate
import com.excilys.ebi.gatling.charts.util.PathHelper.GATLING_CHART_MENU_JS_FILE
import com.excilys.ebi.gatling.charts.writer.TemplateWriter
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_CHARTING_COMPONENT_LIBRARY_CLASS
import com.excilys.ebi.gatling.core.util.FileHelper.HTML_EXTENSION
import com.excilys.ebi.gatling.core.util.FileHelper.formatToFilename
import com.excilys.ebi.gatling.core.util.PathHelper.GATLING_ASSETS_JS_FOLDER
import com.excilys.ebi.gatling.core.util.PathHelper.GATLING_ASSETS_STYLE_FOLDER
import com.excilys.ebi.gatling.core.util.PathHelper.GATLING_JS
import com.excilys.ebi.gatling.core.util.PathHelper.GATLING_RESULTS_FOLDER
import com.excilys.ebi.gatling.core.util.PathHelper.GATLING_STYLE
import com.excilys.ebi.gatling.charts.template.PageTemplate
import org.fusesource.scalate.support.ScalaCompiler

object ReportsGenerator {
	def generateFor(runOn: String) = {
		val dataLoader = new DataLoader(runOn)

		val componentLibrary = getClass.getClassLoader.loadClass(CONFIG_CHARTING_COMPONENT_LIBRARY_CLASS).newInstance.asInstanceOf[ComponentLibrary]

		val reportGenerators =
			List(new ActiveSessionsReportGenerator(runOn, dataLoader, componentLibrary),
				new RequestsReportGenerator(runOn, dataLoader, componentLibrary),
				new RequestDetailsReportGenerator(runOn, dataLoader, componentLibrary))

		copyAssets(runOn)

		generateMenu(runOn, dataLoader)

		reportGenerators.foreach(_.generate)

		PageTemplate.engine.compiler.asInstanceOf[ScalaCompiler].compiler.askShutdown
	}

	private def generateMenu(runOn: String, dataLoader: DataLoader) = {

		val requestLinks: List[(String, Option[String], String)] = dataLoader.requestNames.map {
			requestName =>
				val title = if (requestName.length > 36) Some(requestName.substring(8)) else None
				val printedName = if (requestName.length > 36) requestName.substring(8, 36) + "..." else requestName.substring(8)
				(formatToFilename(requestName) + HTML_EXTENSION, title, printedName)
		}

		val template = new MenuTemplate(requestLinks)

		new TemplateWriter(runOn, GATLING_CHART_MENU_JS_FILE).writeToFile(template.getOutput)
	}

	private def copyAssets(runOn: String) = {
		// Copy all folders/files unders assets to results folder

		val resultFolder = GATLING_RESULTS_FOLDER + "/" + runOn

		val resultStyleAssetsFolderPath = resultFolder + GATLING_STYLE
		val resultStyleAssetsFolder = File(resultStyleAssetsFolderPath).toDirectory
		resultStyleAssetsFolder.createDirectory()

		val styleAssetsFolder = File(GATLING_ASSETS_STYLE_FOLDER).toDirectory
		styleAssetsFolder.deepFiles.foreach {
			file =>
				file.copyTo(resultStyleAssetsFolderPath + "/" + file.name, true)
		}

		val resultJSAssetsFolderPath = resultFolder + GATLING_JS
		val resultJSAssetsFolder = File(resultJSAssetsFolderPath).toDirectory
		resultJSAssetsFolder.createDirectory()

		val jsAssetsFolder = File(GATLING_ASSETS_JS_FOLDER).toDirectory
		jsAssetsFolder.deepFiles.foreach {
			file =>
				file.copyTo(resultJSAssetsFolderPath + "/" + file.name, true)
		}
	}
}