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