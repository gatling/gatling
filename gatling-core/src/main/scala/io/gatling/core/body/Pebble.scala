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

package io.gatling.core.body

import java.util.{ HashMap => JHashMap, Map => JMap }

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import io.gatling.commons.validation._
import io.gatling.core.session.Session

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.extension.writer.PooledSpecializedStringWriter
import com.mitchellbosecke.pebble.loader.StringLoader
import com.mitchellbosecke.pebble.template.PebbleTemplate
import com.typesafe.scalalogging.StrictLogging

object Pebble extends StrictLogging {

  private val Engine = new PebbleEngine.Builder().autoEscaping(false).loader(new StringLoader).build

  private def matchMap(map: Map[String, Any]): JMap[String, AnyRef] = {
    val jMap: JMap[String, AnyRef] = new JHashMap(map.size)
    for ((k, v) <- map) {
      v match {
        case c: Iterable[Any] => jMap.put(k, c.asJava)
        case any: AnyRef      => jMap.put(k, any) //The AnyVal case is not addressed, as an AnyVal will be in an AnyRef wrapper
      }
    }
    jMap
  }

  def parseStringTemplate(string: String): Validation[PebbleTemplate] =
    try {
      Pebble.Engine.getTemplate(string).success
    } catch {
      case NonFatal(e) =>
        logger.error("Error while parsing Pebble string", e)
        e.getMessage.failure
    }

  def evaluateTemplate(template: PebbleTemplate, session: Session): Validation[String] = {
    val context = matchMap(session.attributes)
    val writer = PooledSpecializedStringWriter.pooled
    try {
      template.evaluate(writer, context)
      writer.toString.success
    } catch {
      case NonFatal(e) =>
        logger.info("Error while evaluate Pebble template", e)
        e.getMessage.failure
    }
  }
}
