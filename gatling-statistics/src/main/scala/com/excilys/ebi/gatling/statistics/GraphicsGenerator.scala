package com.excilys.ebi.gatling.statistics

import com.excilys.ebi.gatling.statistics.presenter.ActiveSessionsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.GlobalRequestsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.DetailsRequestsDataPresenter

import java.io.File

import org.apache.commons.io.FileUtils

class GraphicsGenerator {
  def generateFor(runOn: String) = {

    val jQueryFile = new File("assets/js/jquery.min.js")
    val highchartsFile = new File("assets/js/highcharts.js")

    val jQueryFileDest = new File("results/" + runOn + "/js/jquery.min.js")
    val highchartsFileDest = new File("results/" + runOn + "/js/highcharts.js")

    FileUtils.copyFile(jQueryFile, jQueryFileDest)
    FileUtils.copyFile(highchartsFile, highchartsFileDest)

    val menuItems = (new DetailsRequestsDataPresenter).generateGraphFor(runOn)

    (new ActiveSessionsDataPresenter).generateGraphFor(runOn, menuItems)

    (new GlobalRequestsDataPresenter).generateGraphFor(runOn, menuItems)
  }
}