package com.excilys.ebi.gatling.statistics.extractor

import com.excilys.ebi.gatling.core.action.EndAction._
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

    if (actionName != END_OF_SCENARIO) {
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