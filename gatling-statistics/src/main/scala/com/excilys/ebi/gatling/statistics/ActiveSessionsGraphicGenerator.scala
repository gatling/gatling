package com.excilys.ebi.gatling.statistics

import com.excilys.ebi.gatling.statistics.extractor.ActiveSessionsDataExtractor
import com.excilys.ebi.gatling.statistics.presenter.ActiveSessionsDataPresenter

class ActiveSessionsGraphicGenerator extends SimpleGraphicGenerator(new ActiveSessionsDataExtractor, new ActiveSessionsDataPresenter) {

}