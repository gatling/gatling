package com.excilys.ebi.gatling.examples.statistics

import com.excilys.ebi.gatling.statistics.ActiveSessionsDataPresenter

object StatisticsExample {

  def run(runOn: String) = {
    val presenter = new ActiveSessionsDataPresenter
    presenter.generateGraphFor(runOn)
  }
}