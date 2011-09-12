package com.excilys.ebi.gatling.core.config

import com.excilys.ebi.gatling.core.log.Logging
import org.apache.commons.lang3.StringUtils

object GatlingConfig extends Logging {

  val GATLING_HOME = System.getenv("GATLING_HOME")
  val GATLING_SYSTEM_PROPERTY = StringUtils.trimToEmpty(System.getProperty("gatling.config"))

  val loadConfig: GatlingConfiguration =
    try {
      val configFile =
        if (GATLING_SYSTEM_PROPERTY != StringUtils.EMPTY) {
          logger.info("Loading custom configuration file: conf/{}", GATLING_SYSTEM_PROPERTY)
          GATLING_HOME + "/conf/" + GATLING_SYSTEM_PROPERTY
        } else {
          logger.info("Loading default configuration file")
          GATLING_HOME + "/conf/gatling.conf"
        }

      GatlingConfiguration.fromFile(configFile)
    } catch {
      case e =>
        logger.error("{}\n{}", e.getMessage, e.getStackTraceString)
        throw new Exception("Could not parse configuration file.")
    }

  def config = loadConfig

}
