/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.resource

import com.excilys.ebi.gatling.core.log.Logging

/**
 * The ResourceRegistry is responsible for storing a reference
 * to all the resources that has to be closed. It is the responsibility
 * of the developer not to forgive to register its resources.
 */
object ResourceRegistry extends Logging {

  private var resources: Set[Resource] = Set.empty

  /**
   * Registers the resource
   */
  def register(resource: Resource) = {
    logger.debug("Registering {}", resource)
    resources += resource
  }

  /**
   * This method tries to close all the resources properly
   * If one fails the others are still closed and a warning is sent
   * to the logs
   */
  def closeAll = {
    for (resource <- resources) {
      logger.debug("Closing {}", resource)
      try {
        resource.close
      } catch {
        case e => logger.warn("Could not close resource {}: {}", resource, e)
      }
    }
  }
}
