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
package io.gatling.core.feeder

import java.io.InputStream
import java.net.URL
import java.util.{ Collection => JCollection, Map => JMap }

import scala.collection._
import scala.collection.JavaConversions._

import io.gatling.commons.util.Io._
import io.gatling.core.json.JsonParsers
import io.gatling.core.util.Resource

class JsonFeederFileParser(implicit jsonParsers: JsonParsers) {

  def parse(resource: Resource): IndexedSeq[Record[Any]] =
    withCloseable(resource.inputStream) { is =>
      stream(is).toVector
    }

  def url(url: String): IndexedSeq[Record[Any]] =
    withCloseable(new URL(url).openStream) { is =>
      stream(is).toVector
    }

  def stream(is: InputStream): Iterator[Record[Any]] = {

    jsonParsers.jackson.parse(is) match {

      case array: JCollection[_] =>

        array.iterator.collect {
          case element: JMap[_, _] =>
            // type erasure I love u
            element.asInstanceOf[JMap[String, _]].toMap
        }

      case _ => throw new IllegalArgumentException("Root element of JSON feeder file isn't an array")
    }
  }
}
