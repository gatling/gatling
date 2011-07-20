package com.excilys.ebi.gatling.examples.statistics

import com.excilys.ebi.gatling.statistics.ActiveSessionsDataPresenter
import com.excilys.ebi.gatling.statistics.GlobalRequestsDataPresenter
import com.excilys.ebi.gatling.statistics.DetailsRequestsDataPresenter

object StatisticsExample {

  def run(runOn: String) = {
    val activeSessionsPresenter = new ActiveSessionsDataPresenter
    activeSessionsPresenter.generateGraphFor(runOn)

    val requestsPresenter = new GlobalRequestsDataPresenter
    requestsPresenter.generateGraphFor(runOn)

    val detailsRequestsPresenter = new DetailsRequestsDataPresenter
    detailsRequestsPresenter.generateGraphFor(runOn)
  }
}