package io.gatling.core.json

import java.io.InputStream
import java.nio.charset.Charset

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.validation._

object JsonParsers {

  def apply()(implicit configuration: GatlingConfiguration) =
    new JsonParsers(Jackson(), new Boon, configuration.core.extract.jsonPath.preferJackson)
}

case class JsonParsers(jackson: Jackson, boon: Boon, preferJackson: Boolean) {

  private val JacksonErrorMapper: String => String = "Jackson failed to parse into a valid AST: " + _
  private val BoonErrorMapper: String => String = "Boon failed to parse into a valid AST: " + _

  def safeParseJackson(string: String): Validation[Object] =
    executeSafe(JacksonErrorMapper)(jackson.parse(string).success)

  def safeParseJackson(is: InputStream, charset: Charset): Validation[Object] =
    executeSafe(JacksonErrorMapper)(jackson.parse(is, charset).success)

  def safeParseBoon(string: String): Validation[Object] =
    executeSafe(BoonErrorMapper)(boon.parse(string).success)

  def safeParse(string: String): Validation[Object] =
    if (preferJackson)
      safeParseJackson(string)
    else
      safeParseBoon(string)
}
