package com.excilys.ebi.gatling.statistics

import com.excilys.ebi.gatling.statistics.presenter.ActiveSessionsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.GlobalRequestsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.DetailsRequestsDataPresenter

class GraphicsGenerator {
  def generateFor(runOn: String) = {
    val menuItems = (new DetailsRequestsDataPresenter).generateGraphFor(runOn)

    (new ActiveSessionsDataPresenter).generateGraphFor(runOn, menuItems)

    (new GlobalRequestsDataPresenter).generateGraphFor(runOn, menuItems)
  }
}