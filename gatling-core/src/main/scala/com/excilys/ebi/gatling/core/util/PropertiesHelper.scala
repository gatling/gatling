package com.excilys.ebi.gatling.core.util

import org.apache.commons.lang3.StringUtils

object PropertiesHelper {
  val GATLING_CONFIG_PROPERTY = StringUtils.trimToEmpty(System.getProperty("gatling.config"))
  val NO_STATS_PROPERTY = System.getProperty("NoStats", "false")
  val ONLY_STATS_PROPERTY = System.getProperty("OnlyStats", "false")
}