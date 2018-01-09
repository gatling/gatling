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

import java.lang.{ StringBuilder => JStringBuilder }
import java.util.{ Collection => JCollection, Map => JMap }

import scala.annotation.switch
import scala.collection.JavaConverters._

import io.gatling.commons.util.Maps._
import io.gatling.commons.util.StringBuilderPool

import com.fasterxml.jackson.databind.ObjectMapper

object Json {

  private val objectMapper = new ObjectMapper
  private val stringBuilders = new StringBuilderPool

  def stringify(value: Any, isRootObject: Boolean = true): String =
    stringBuilders.get().appendStringified(value, isRootObject).toString

  def asScala(value: Any): Any =
    value match {
      case list: JCollection[_] => list.asScala.map(asScala)
      case map: JMap[_, _] =>
        (map.size: @switch) match {
          case 0 => Map.empty
          case 1 =>
            val entry0 = map.entrySet.iterator.next()
            new Map.Map1(entry0.getKey, asScala(entry0.getValue))
          case 2 =>
            val it = map.entrySet.iterator
            val entry0 = it.next()
            val entry1 = it.next()
            new Map.Map2(
              entry0.getKey, asScala(entry0.getValue),
              entry1.getKey, asScala(entry1.getValue)
            )
          case 3 =>
            val it = map.entrySet.iterator
            val entry0 = it.next()
            val entry1 = it.next()
            val entry2 = it.next()
            new Map.Map3(
              entry0.getKey, asScala(entry0.getValue),
              entry1.getKey, asScala(entry1.getValue),
              entry2.getKey, asScala(entry2.getValue)
            )
          case 4 =>
            val it = map.entrySet.iterator
            val entry0 = it.next()
            val entry1 = it.next()
            val entry2 = it.next()
            val entry3 = it.next()
            new Map.Map4(
              entry0.getKey, asScala(entry0.getValue),
              entry1.getKey, asScala(entry1.getValue),
              entry2.getKey, asScala(entry2.getValue),
              entry3.getKey, asScala(entry3.getValue)
            )
          case _ =>
            map.asScala.toMap.forceMapValues(asScala)
        }

      case _ => value
    }

  implicit class JsonStringBuilder(val sb: JStringBuilder) extends AnyVal {

    def appendStringified(value: Any, rootLevel: Boolean): JStringBuilder = value match {
      case b: Byte                   => appendValue(b)
      case s: Short                  => appendValue(s)
      case i: Int                    => appendValue(i)
      case l: Long                   => appendValue(l)
      case f: Float                  => appendValue(f)
      case d: Double                 => appendValue(d)
      case bool: Boolean             => appendValue(bool)
      case s: String                 => appendString(s, rootLevel)
      case null                      => appendNull()
      case map: collection.Map[_, _] => appendMap(map)
      case jMap: JMap[_, _]          => appendMap(jMap.asScala)
      case array: Array[_]           => appendArray(array)
      case seq: Seq[_]               => appendArray(seq)
      case coll: JCollection[_]      => appendArray(coll.asScala)
      case any                       => appendString(any.toString, rootLevel)
    }

    private def appendString(s: String, rootLevel: Boolean): JStringBuilder = {
      val escapedAndWrapped = objectMapper.writeValueAsString(s)
      if (rootLevel) {
        sb.append(escapedAndWrapped, 1, escapedAndWrapped.length - 1)
      } else {
        sb.append(escapedAndWrapped)
      }
    }

    private def appendValue(value: Any): JStringBuilder =
      sb.append(value)

    private def appendNull(): JStringBuilder =
      sb.append("null")

    private def appendArray(iterable: Traversable[_]): JStringBuilder = {
      sb.append('[')
      iterable.foreach { elem =>
        appendStringified(elem, rootLevel = false).append(',')
      }
      if (iterable.nonEmpty) {
        sb.setLength(sb.length - 1)
      }
      sb.append(']')
    }

    private def appendMap(map: collection.Map[_, _]): JStringBuilder = {
      sb.append('{')
      map.foreach {
        case (key, value) =>
          sb.append('"').append(key).append("\":")
          appendStringified(value, rootLevel = false).append(',')
      }
      if (map.nonEmpty) {
        sb.setLength(sb.length - 1)
      }
      sb.append('}')
    }
  }
}
