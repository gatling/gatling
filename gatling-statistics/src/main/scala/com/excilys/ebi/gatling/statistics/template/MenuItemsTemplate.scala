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
package com.excilys.ebi.gatling.statistics.template

import com.excilys.ebi.gatling.core.util.PathHelper._

import org.fusesource.scalate._

class MenuItemsTemplate(val requestLinks: List[(String, String)], val groupLinks: List[(String, String)]) {
	val engine = new TemplateEngine
	engine.escapeMarkup = false

	def getOutput: String = {

		var requestLinksHtml = ""
		var groupLinksHtml = ""

		for (requestLink <- requestLinks)
			requestLinksHtml += "<li><a href='" + requestLink._1 + "'>" + requestLink._2 + "</a></li>"

		for (groupLink <- groupLinks)
			groupLinksHtml += "<li><a href='" + groupLink._1 + "'>" + groupLink._2 + "</a></li>"

		engine.layout(GATLING_TEMPLATE_MENU_JS_FILE,
			Map("requestLinks" -> requestLinksHtml,
				"groupLinks" -> groupLinksHtml))
	}
}