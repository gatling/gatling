package com.excilys.ebi.gatling.statistics.generator

import com.excilys.ebi.gatling.statistics.presenter.MenuItemsDataPresenter
import com.excilys.ebi.gatling.statistics.extractor.MenuItemsDataExtractor

class MenuItemsGraphicGenerator extends SimpleGraphicGenerator(new MenuItemsDataExtractor, new MenuItemsDataPresenter)