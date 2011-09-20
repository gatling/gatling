package com.excilys.ebi.gatling.statistics

import com.excilys.ebi.gatling.core.util.PathHelper._

import com.excilys.ebi.gatling.statistics.presenter.ActiveSessionsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.GlobalRequestsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.DetailsRequestsDataPresenter

import java.io.File

import org.apache.commons.io.FileUtils

class GraphicsGenerator {
  def generateFor(runOn: String) = {

    val jQueryFile = new File(GATLING_ASSETS_JQUERY)
    val highchartsFile = new File(GATLING_ASSETS_HIGHCHARTS)

    val jQueryFileDest = new File(GATLING_RESULTS_FOLDER + "/" + runOn + GATLING_JS_JQUERY)
    val highchartsFileDest = new File(GATLING_RESULTS_FOLDER + "/" + runOn + GATLING_JS_HIGHCHARTS)

    FileUtils.copyFile(jQueryFile, jQueryFileDest)
    FileUtils.copyFile(highchartsFile, highchartsFileDest)

    val menuItems = (new DetailsRequestsDataPresenter).generateGraphFor(runOn)

    (new ActiveSessionsDataPresenter).generateGraphFor(runOn, menuItems)

    (new GlobalRequestsDataPresenter).generateGraphFor(runOn, menuItems)
  }
}