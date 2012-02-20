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
package com.excilys.ebi.gatling.charts.template

import org.fusesource.scalate.TemplateEngine
import org.joda.time.DateTime

import com.excilys.ebi.gatling.charts.component.Component
import com.excilys.ebi.gatling.charts.config.ChartsFiles.{ MENU_FILE, JQUERY_FILE, GATLING_TEMPLATE_LAYOUT_FILE_URL }
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.DateHelper.printReadableDate

object PageTemplate {
	val TEMPLATE_ENGINE = new TemplateEngine
	TEMPLATE_ENGINE.allowReload = false
	TEMPLATE_ENGINE.escapeMarkup = false

	private var runOn: DateTime = _
	def setRunOn(runOn: DateTime) { PageTemplate.runOn = runOn }
}

abstract class PageTemplate(title: String, isDetails: Boolean, components: Component*) extends Logging {

	val jsFiles = (Seq(JQUERY_FILE, MENU_FILE) ++ getAdditionnalJSFiles).distinct

	def getContent: String = components.map(_.getHTMLContent).mkString

	def getJavascript: String = components.map(_.getJavascriptContent).mkString

	def getAdditionnalJSFiles = components.map(_.getJavascriptFiles).flatten.toSeq

	def getOutput: String = {
		PageTemplate.TEMPLATE_ENGINE.layout(GATLING_TEMPLATE_LAYOUT_FILE_URL,
			Map("jsFiles" -> jsFiles,
				"pageTitle" -> title,
				"pageContent" -> getContent,
				"javascript" -> getJavascript,
				"isDetails" -> isDetails,
				"simulationDate" -> printReadableDate(PageTemplate.runOn)))
	}
}