package com.excilys.ebi.gatling.core.config

import com.excilys.ebi.gatling.core.log.Logging

object GatlingConfig extends Logging {
  val loadConfig: GatlingConfiguration =
    try {
      val configFile =
        if (System.getProperty("gatling.config", "") != "") {
          logger.info("Loading custom configuration file: conf/{}", System.getProperty("gatling.config"))
          System.getenv("GATLING_HOME") + "/conf/" + System.getProperty("gatling.config")
        } else {
          logger.info("Loading default configuration file")
          System.getenv("GATLING_HOME") + "/conf/gatling.conf"
        }

      GatlingConfiguration.fromFile(configFile)
    } catch {
      case e =>
        logger.error("{}\n{}", e.getMessage, e.getStackTraceString)
        throw new Exception("Could not parse configuration file.")
    }

  def config = loadConfig

}
