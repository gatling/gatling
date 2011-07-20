package com.excilys.ebi.gatling.examples.statistics

import com.excilys.ebi.gatling.statistics.presenter.ActiveSessionsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.GlobalRequestsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.DetailsRequestsDataPresenter

object StatisticsExample {

  def run(runOn: String) = {
    val detailsRequestsPresenter = new DetailsRequestsDataPresenter
    val menuItems = detailsRequestsPresenter.generateGraphFor(runOn)

    val activeSessionsPresenter = new ActiveSessionsDataPresenter
    activeSessionsPresenter.generateGraphFor(runOn, menuItems)

    val requestsPresenter = new GlobalRequestsDataPresenter
    requestsPresenter.generateGraphFor(runOn, menuItems)

  }
}