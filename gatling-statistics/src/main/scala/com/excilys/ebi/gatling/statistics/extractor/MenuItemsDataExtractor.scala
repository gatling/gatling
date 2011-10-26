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
package com.excilys.ebi.gatling.statistics.extractor

import com.excilys.ebi.gatling.core.action.EndAction._
import com.excilys.ebi.gatling.core.action.StartAction._
import com.excilys.ebi.gatling.core.util.FileHelper._

import com.excilys.ebi.gatling.statistics.MenuItemType._

class MenuItemsDataExtractor extends DataExtractor[List[(MenuItemType, String, String)]] {
	var items: List[(MenuItemType, String, String)] = Nil

	var requestNames: List[String] = Nil
	var groupNames: List[String] = Nil

	def addItem(itemType: MenuItemType, fileName: String, friendlyName: String) = {
		items = (itemType, fileName, friendlyName) :: items
	}

	def onRow(runOn: String, scenarioName: String, userId: String, actionName: String, executionStartDate: String, executionDuration: String, resultStatus: String, resultMessage: String, groups: List[String]) = {

		if (actionName != END_OF_SCENARIO && actionName != START_OF_SCENARIO) {
			val requestName = actionName.substring(8)

			if (!requestNames.contains(requestName)) {
				requestNames = requestName :: requestNames
				items = (REQUEST_DETAILS, formatToFilename(actionName) + HTML_EXTENSION, requestName) :: items
			}

			for (groupName <- groups) {
				if (!groupNames.contains(groupName)) {
					groupNames = groupName :: groupNames
					items = (GROUP, formatToFilename(actionName) + HTML_EXTENSION, groupName) :: items
				}
			}
		}

	}

	def getResults: List[(MenuItemType, String, String)] = {
		items
	}
}