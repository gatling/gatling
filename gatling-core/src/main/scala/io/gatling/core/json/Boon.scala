/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.JavaRuntime

import io.advantageous.boon.json.implementation.{ JsonFastParser, JsonParserUsingCharacterSource }

class Boon extends JsonParser {

  if (!JavaRuntime.IsJava8) {
    // disable io.advantageous.boon.core.reflection.FastStringUtils that doesn't work with Java 9+
    System.setProperty("io.advantageous.boon.faststringutils.disable", java.lang.Boolean.toString(true))
  }

  private def newFastParser = new JsonFastParser(false, false, true, false)

  override def parse(bytes: Array[Byte], charset: Charset): AnyRef = {
    val parser = newFastParser
    parser.setCharset(charset)
    parser.parse(bytes)
  }

  override def parse(string: String): AnyRef =
    newFastParser.parse(string)

  override def parse(stream: InputStream, charset: Charset): AnyRef =
    new JsonParserUsingCharacterSource().parse(new InputStreamReader(stream, charset))
}
