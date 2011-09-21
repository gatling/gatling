package com.excilys.ebi.gatling.statistics.presenter
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer

abstract class DataPresenter[R] {
  protected def getDateForHighcharts(date: String): String = {
    "Date.UTC(" + date.substring(0, 4) + ", " + date.substring(5, 7) + ", " + date.substring(8, 10) +
      ", " + date.substring(11, 13) + ", " + date.substring(14, 16) + ", " + date.substring(17, 19) + ")"
  }

  def generateGraphFor(runOn: String, results: R, menuItems: Map[String, String])
}