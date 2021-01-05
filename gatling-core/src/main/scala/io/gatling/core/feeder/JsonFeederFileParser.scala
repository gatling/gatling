/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.core.feeder

import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset

import scala.jdk.CollectionConverters._
import scala.util.Using

import io.gatling.commons.util.Io._
import io.gatling.core.json.{ Json, JsonParsers }
import io.gatling.core.util.Resource

class JsonFeederFileParser(jsonParsers: JsonParsers) {

  def parse(resource: Resource, charset: Charset): IndexedSeq[Record[Any]] =
    Using.resource(resource.inputStream) { is =>
      stream(is, charset).toVector
    }

  def url(url: String, charset: Charset): IndexedSeq[Record[Any]] =
    Using.resource(new URL(url).openStream) { is =>
      stream(is, charset).toVector
    }

  def stream(is: InputStream, charset: Charset): Iterator[Record[Any]] = {
    val node = jsonParsers.parse(is, charset)
    if (node.isArray) {
      node.elements.asScala.collect {
        case node if node.isObject => Json.asScala(node).asInstanceOf[collection.immutable.Map[String, Any]]
      }
    } else {
      throw new IllegalArgumentException("Root element of JSON feeder file isn't an array")
    }
  }
}
