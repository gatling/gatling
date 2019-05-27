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

import java.io.{ InputStream, InputStreamReader }
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.{ UTF_16, UTF_8 }

import io.gatling.commons.util.NonStandardCharsets.UTF_32
import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration

import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }

object JsonParsers {

  private val JacksonErrorMapper: String => String = "Jackson failed to parse into a valid AST: " + _
  private val JsonSupportedEncodings = Vector(UTF_8, UTF_16, UTF_32)

  def apply()(implicit configuration: GatlingConfiguration) =
    new JsonParsers(new ObjectMapper, configuration.core.charset)
}

class JsonParsers(objectMapper: ObjectMapper, defaultCharset: Charset) {

  import JsonParsers._

  def parse(is: InputStream, charset: Charset = defaultCharset): JsonNode =
    if (JsonParsers.JsonSupportedEncodings.contains(charset)) {
      objectMapper.readValue(is, classOf[JsonNode])
    } else {
      val reader = new InputStreamReader(is, charset)
      objectMapper.readValue(reader, classOf[JsonNode])
    }

  def safeParse(is: InputStream, charset: Charset = defaultCharset): Validation[JsonNode] =
    safely(JacksonErrorMapper)(parse(is, charset).success)

  def parse(string: String): JsonNode =
    objectMapper.readValue(string, classOf[JsonNode])

  def safeParse(string: String): Validation[JsonNode] =
    safely(JacksonErrorMapper)(parse(string).success)
}
