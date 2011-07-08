package com.excilys.ebi.gatling.examples.statistics

import com.excilys.ebi.gatling.statistics.ActiveSessionsDataPresenter
import com.excilys.ebi.gatling.statistics.RequestsDataPresenter

object StatisticsExample {

  def run(runOn: String) = {
    val activeSessionsPresenter = new ActiveSessionsDataPresenter
    activeSessionsPresenter.generateGraphFor(runOn)

    val requestsPresenter = new RequestsDataPresenter
    requestsPresenter.generateGraphFor(runOn)
  }
}