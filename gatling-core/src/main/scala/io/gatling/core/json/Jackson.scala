/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import java.nio.charset.StandardCharsets._

import io.gatling.commons.util.FastByteArrayInputStream
import io.gatling.commons.util.NonStandardCharsets.UTF_32
import io.gatling.core.config.GatlingConfiguration

import com.fasterxml.jackson.databind.ObjectMapper

object Jackson {

  def apply()(implicit configuration: GatlingConfiguration) =
    new Jackson(new ObjectMapper, configuration.core.charset)
}

class Jackson(objectMapper: ObjectMapper, defaultCharset: Charset) extends JsonParser {

  val JsonSupportedEncodings = Vector(UTF_8, UTF_16, UTF_32)

  def parse(bytes: Array[Byte], charset: Charset): Object =
    if (JsonSupportedEncodings.contains(charset)) {
      objectMapper.readValue(bytes, classOf[Object])
    } else {
      val reader = new InputStreamReader(new FastByteArrayInputStream(bytes), charset)
      objectMapper.readValue(reader, classOf[Object])
    }

  def parse(string: String): Object = objectMapper.readValue(string, classOf[Object])

  def parse(stream: InputStream, charset: Charset = defaultCharset): Object =
    if (JsonSupportedEncodings.contains(charset)) {
      objectMapper.readValue(stream, classOf[Object])
    } else {
      val reader = new InputStreamReader(stream, charset)
      objectMapper.readValue(reader, classOf[Object])
    }
}
