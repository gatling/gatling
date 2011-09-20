package com.excilys.ebi.gatling.core.config

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.util.PropertiesHelper._

import org.apache.commons.lang3.StringUtils

object GatlingConfig extends Logging {

  val loadConfig: GatlingConfiguration =
    try {
      val configFile =
        if (GATLING_CONFIG_PROPERTY != StringUtils.EMPTY) {
          logger.info("Loading custom configuration file: conf/{}", GATLING_CONFIG_PROPERTY)
          GATLING_CONFIG_FOLDER + "/" + GATLING_CONFIG_PROPERTY
        } else {
          logger.info("Loading default configuration file")
          GATLING_CONFIG_FOLDER + "/" + GATLING_CONFIG_FILE
        }

      GatlingConfiguration.fromFile(configFile)
    } catch {
      case e =>
        logger.error("{}\n{}", e.getMessage, e.getStackTraceString)
        throw new Exception("Could not parse configuration file.")
    }

  def config = loadConfig

  val CONFIG_GATLING_FEEDER_ENCODING = config.getString("gatling.feeders.encoding", "utf-8")
}
