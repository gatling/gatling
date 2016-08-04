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

import java.util.{ Collection => JCollection, Map => JMap }

import scala.collection.JavaConversions.{ collectionAsScalaIterable, mapAsScalaMap }

import com.dongxiguo.fastring.Fastring.Implicits._

object Json {

  def stringify(value: Any, isRootObject: Boolean = true): String =
    fastringify(value, isRootObject).toString()

  private def fastringify(value: Any, rootLevel: Boolean): Fastring = value match {
    case b: Byte                   => writeValue(b)
    case s: Short                  => writeValue(s)
    case i: Int                    => writeValue(i)
    case l: Long                   => writeValue(l)
    case f: Float                  => writeValue(f)
    case d: Double                 => writeValue(d)
    case bool: Boolean             => writeValue(bool)
    case s: String                 => writeString(s, rootLevel)
    case null                      => writeNull
    case map: collection.Map[_, _] => writeMap(map)
    case jMap: JMap[_, _]          => writeMap(jMap)
    case array: Array[_]           => writeArray(array)
    case seq: Seq[_]               => writeArray(seq)
    case coll: JCollection[_]      => writeArray(coll)
    case any                       => writeString(any.toString, rootLevel)
  }

  private def writeString(s: String, rootLevel: Boolean) = {
    val escapedLineFeeds = s.replace("\n", "\\n")
    if (rootLevel) fast"$escapedLineFeeds" else fast""""$escapedLineFeeds""""
  }

  private def writeValue(value: Any) = fast"${value.toString}"

  private def writeNull = fast"null"

  private def writeArray(iterable: Traversable[_]) = fast"[${iterable.map(elem => fastringify(elem, false)).mkFastring(",")}]"

  private def writeMap(map: collection.Map[_, _]) = {
      def serializeEntry(key: String, value: Any) = fast""""$key":${fastringify(value, false)}"""
    fast"{${map.map { case (key, value) => serializeEntry(key.toString, value) }.mkFastring(",")}}"
  }
}
