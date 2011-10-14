package com.excilys.ebi.gatling.core.resource

import com.excilys.ebi.gatling.core.log.Logging

object ResourceRegistry extends Logging {

  private var resources: Set[Resource] = Set.empty

  def register(resource: Resource) = {
    logger.warn("Registering {}", resource)
    resources += resource
  }

  def closeAll = {
    for (resource <- resources) {
      logger.warn("Closing {}", resource)
      try {
        resource.close
      } catch {
        case e => logger.warn("Could not close resource {}: {}", resource, e)
      }
    }
  }
}
