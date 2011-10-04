package com.excilys.ebi.gatling.core.util

import org.apache.commons.lang3.StringUtils

object PropertiesHelper {
  val GATLING_CONFIG_PROPERTY = Option(System.getProperty("gatling.config")) map (_.trim()) getOrElse StringUtils.EMPTY
  val NO_STATS_PROPERTY = Option(System.getProperty("NoStats")) map (_.toBoolean) getOrElse false
  val ONLY_STATS_PROPERTY = Option(System.getProperty("OnlyStats")) map (_.toBoolean) getOrElse false
}