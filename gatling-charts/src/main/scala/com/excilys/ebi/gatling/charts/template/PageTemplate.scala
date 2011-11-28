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
package com.excilys.ebi.gatling.charts.template
import org.fusesource.scalate.TemplateEngine

import com.excilys.ebi.gatling.charts.component.Component
import com.excilys.ebi.gatling.charts.config.ChartsConfig._
import com.excilys.ebi.gatling.core.log.Logging

object PageTemplate {
	val TEMPLATE_ENGINE = new TemplateEngine
	TEMPLATE_ENGINE.allowReload = false
	TEMPLATE_ENGINE.escapeMarkup = false
}

abstract class PageTemplate(title: String, subTitle: String, components: Component*) extends Logging {

	val jsFiles = (Seq(JQUERY_FILE, MENU_FILE) ++ getAdditionnalJSFiles).distinct

	def getContent: String = (for (component <- components) yield component.getHTMLContent).mkString

	def getJavascript: String = (for (component <- components) yield component.getJavascriptContent).mkString

	def getAdditionnalJSFiles = (for (component <- components) yield component.getJavascriptFiles).flatten.toSeq

	def getOutput: String = {
		PageTemplate.TEMPLATE_ENGINE.layout(GATLING_TEMPLATE_LAYOUT_FILE,
			Map("jsFiles" -> jsFiles,
				"pageTitle" -> title,
				"pageSubTitle" -> subTitle,
				"pageContent" -> getContent,
				"javascript" -> getJavascript))
	}
}