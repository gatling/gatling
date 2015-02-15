package io.gatling.core.json

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.validation._

class JsonParsers(implicit configuration: GatlingConfiguration) {

  val jackson: Jackson = new Jackson

  val boon = Boon

  val preferJackson = configuration.core.extract.jsonPath.preferJackson

  def safeParse(string: String): Validation[Object] =
    if (preferJackson)
      executeSafe(jackson.parse(string).success)
    else
      executeSafe(boon.parse(string).success)
}
