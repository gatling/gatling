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
package com.excilys.ebi.gatling.statistics.presenter

import com.excilys.ebi.gatling.core.util.PathHelper._

import com.excilys.ebi.gatling.statistics.MenuItemType._
import com.excilys.ebi.gatling.statistics.template.MenuItemsTemplate
import com.excilys.ebi.gatling.statistics.writer.TemplateWriter

class MenuItemsDataPresenter extends DataPresenter[List[(MenuItemType, String, String)]] {

	def generateChartFor(runOn: String, results: List[(MenuItemType, String, String)]) = {
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

		new TemplateWriter(runOn, GATLING_CHART_MENU_JS_FILE).writeToFile(output)
	}
}