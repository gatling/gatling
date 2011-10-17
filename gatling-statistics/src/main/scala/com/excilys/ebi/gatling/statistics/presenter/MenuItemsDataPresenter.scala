package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.util.PathHelper._

import com.excilys.ebi.gatling.statistics.MenuItemType._
import com.excilys.ebi.gatling.statistics.template.MenuItemsTemplate
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter

class MenuItemsDataPresenter extends DataPresenter[List[(MenuItemType, String, String)]] {

	def generateGraphFor(runOn: String, results: List[(MenuItemType, String, String)]) = {
		var requestLinks: List[(String, String)] = Nil
		var groupLinks: List[(String, String)] = Nil

		results.foreach {
			case (itemType, fileName, itemName) =>
				itemType match {
					case GROUP => groupLinks = (fileName, itemName) :: groupLinks
					case REQUEST_DETAILS => requestLinks = (fileName, itemName) :: requestLinks
				}
		}

		val output = new MenuItemsTemplate(requestLinks, groupLinks).getOutput

		new TemplateWriter(runOn, GATLING_GRAPH_MENU_JS_FILE).writeToFile(output)
	}
}