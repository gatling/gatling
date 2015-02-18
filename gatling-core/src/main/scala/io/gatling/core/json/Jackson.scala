/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
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

import com.fasterxml.jackson.jr.ob.JSON
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.FastByteArrayInputStream
import io.gatling.core.util.NonStandardCharsets.UTF_32

object Jackson {
  val JsonSupportedEncodings = Vector(UTF_8, UTF_16, UTF_32)
}

class Jackson(implicit configuration: GatlingConfiguration) extends JsonParser {

  import Jackson.JsonSupportedEncodings

  val jsonJr: JSON = JSON.std
    .`with`(JSON.Feature.READ_ONLY)
    .`without`(JSON.Feature.HANDLE_JAVA_BEANS)

  def parse(bytes: Array[Byte], charset: Charset) =
    if (JsonSupportedEncodings.contains(charset)) {
      jsonJr.anyFrom(bytes)
    } else {
      val reader = new InputStreamReader(new FastByteArrayInputStream(bytes), charset)
      jsonJr.anyFrom(reader)
    }

  def parse(string: String) = jsonJr.anyFrom(string)

  def parse(stream: InputStream, charset: Charset = configuration.core.charset) =
    if (JsonSupportedEncodings.contains(charset)) {
      jsonJr.anyFrom(stream)
    } else {
      val reader = new InputStreamReader(stream, charset)
      jsonJr.anyFrom(reader)
    }
}
