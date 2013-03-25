/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import com.excilys.ebi.gatling.charts.component.Component
import com.excilys.ebi.gatling.charts.config.ChartsFiles.{ ALL_SESSIONS_FILE, BOOTSTRAP_FILE, GATLING_JS_FILE, GATLING_TEMPLATE_LAYOUT_FILE_URL, JQUERY_FILE, MENU_FILE, STATS_JS_FILE }
import com.excilys.ebi.gatling.core.result.message.RunRecord

object PageTemplate {
	val TEMPLATE_ENGINE = {
		val engine = new TemplateEngine
		engine.allowReload = false
		engine.escapeMarkup = false
		engine
	}

	private var runRecord: RunRecord = _
	private var runStart: Long = _
	private var runEnd: Long = _

	def setRunInfo(runRecord: RunRecord,runStart: Long,runEnd: Long) {
		PageTemplate.runRecord = runRecord
		PageTemplate.runStart = runStart
		PageTemplate.runEnd = runEnd
	}
}

abstract class PageTemplate(title: String, isDetails: Boolean, components: Component*) {

	val jsFiles: Seq[String] = (Seq(JQUERY_FILE, BOOTSTRAP_FILE, GATLING_JS_FILE, MENU_FILE, ALL_SESSIONS_FILE, STATS_JS_FILE) ++ getAdditionnalJSFiles).distinct

	def getContent: String = components.map(_.getHTMLContent).mkString

	def getJavascript: String = components.map(_.getJavascriptContent).mkString

	def getAdditionnalJSFiles: Seq[String] = components.flatMap(_.getJavascriptFiles)

	def getAttributes: Map[String, Any] =
		Map("jsFiles" -> jsFiles,
			"pageTitle" -> title,
			"pageContent" -> getContent,
			"javascript" -> getJavascript,
			"isDetails" -> isDetails,
			"runRecord" -> PageTemplate.runRecord,
			"runStart" -> PageTemplate.runStart,
			"runEnd" -> PageTemplate.runEnd)

	def getOutput: String = {
		PageTemplate.TEMPLATE_ENGINE.layout(GATLING_TEMPLATE_LAYOUT_FILE_URL, getAttributes)
	}
}