/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.json

import java.io.InputStream
import java.nio.charset.Charset

import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration

object JsonParsers {

  def apply()(implicit configuration: GatlingConfiguration) =
    new JsonParsers(Jackson(), new JoddJson, configuration.core.extract.jsonPath.preferJackson)
}

case class JsonParsers(jackson: Jackson, jodd: JoddJson, preferJackson: Boolean) {

  private val JacksonErrorMapper: String => String = "Jackson failed to parse into a valid AST: " + _
  private val JoddErrorMapper: String => String = "Jodd failed to parse into a valid AST: " + _

  private def safeParseJackson(string: String): Validation[Object] =
    safely(JacksonErrorMapper)(jackson.parse(string).success)

  def safeParseJackson(is: InputStream, charset: Charset): Validation[Object] =
    safely(JacksonErrorMapper)(jackson.parse(is, charset).success)

  def safeParseJodd(string: String): Validation[Object] =
    safely(JoddErrorMapper)(jodd.parse(string).success)

  def safeParse(string: String): Validation[Object] =
    if (preferJackson)
      safeParseJackson(string)
    else
      safeParseJodd(string)
}
