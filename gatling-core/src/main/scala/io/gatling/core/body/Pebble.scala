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

package io.gatling.core.body

import java.{ util => ju }

import scala.collection.immutable
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

import io.gatling.commons.validation._
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.util.{ ClasspathFileResource, ClasspathPackagedResource, FilesystemResource, Resource }

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.extension.Extension
import com.mitchellbosecke.pebble.extension.writer.PooledSpecializedStringWriter
import com.mitchellbosecke.pebble.loader.StringLoader
import com.mitchellbosecke.pebble.template.PebbleTemplate
import com.typesafe.scalalogging.StrictLogging

private[gatling] object PebbleExtensions {

  private[body] var extensions: Seq[Extension] = Nil

  def register(extensions: Seq[Extension]): Unit = {
    if (this.extensions.nonEmpty) {
      throw new UnsupportedOperationException("Pebble extensions have already been registered")
    }
    this.extensions = extensions
  }
}

private[gatling] object Pebble extends StrictLogging {

  private val StringEngine = new PebbleEngine.Builder().autoEscaping(false).extension(PebbleExtensions.extensions: _*).loader(new StringLoader).build
  private val DelegatingEngine = new PebbleEngine.Builder().autoEscaping(false).extension(PebbleExtensions.extensions: _*).build

  private def mutableSeqToJava(c: mutable.Seq[_]): ju.List[AnyRef] =
    c.map(anyRefToJava).asJava

  private def immutableSeqToJava(c: immutable.Seq[_]): ju.List[AnyRef] =
    c.map(anyRefToJava).asJava

  private def mutableSetToJava(c: mutable.Set[_]): ju.Set[AnyRef] =
    c.map(anyRefToJava).asJava

  private def immutableSetToJava(c: immutable.Set[_]): ju.Set[AnyRef] =
    c.map(anyRefToJava).asJava

  private def mutableMapToJava(c: mutable.Map[_, _]): ju.Map[_, AnyRef] =
    (Map.empty ++ c.view.mapValues(anyRefToJava)).asJava

  private def immutableMapToJava(c: immutable.Map[_, _]): ju.Map[_, AnyRef] =
    (Map.empty ++ c.view.mapValues(anyRefToJava)).asJava

  private def anyRefToJava(any: Any): AnyRef = any match {
    case c: mutable.Seq[_]      => mutableSeqToJava(c)
    case c: immutable.Seq[_]    => immutableSeqToJava(c)
    case s: mutable.Set[_]      => mutableSetToJava(s)
    case s: immutable.Set[_]    => immutableSetToJava(s)
    case m: mutable.Map[_, _]   => mutableMapToJava(m)
    case m: immutable.Map[_, _] => immutableMapToJava(m)
    case anyRef: AnyRef         => anyRef // the AnyVal case is not addressed, as an AnyVal will be in an AnyRef wrapper
  }

  private[body] def sessionAttributesToJava(map: Map[String, Any]): ju.Map[String, AnyRef] = {
    val jMap = new ju.HashMap[String, AnyRef](map.size)
    for ((k, v) <- map if !k.startsWith(SessionPrivateAttributes.PrivateAttributePrefix)) {
      jMap.put(k, anyRefToJava(v))
    }
    jMap
  }

  def getStringTemplate(string: String): Validation[PebbleTemplate] =
    try {
      StringEngine.getTemplate(string).success
    } catch {
      case NonFatal(e) =>
        logger.error("Error while parsing Pebble string", e)
        e.getMessage.failure
    }

  def getResourceTemplate(resource: Resource): Validation[PebbleTemplate] =
    try {
      val templateName = resource match {
        case ClasspathPackagedResource(path, _) => path
        case ClasspathFileResource(path, _)     => path
        case FilesystemResource(file)           => file.getAbsolutePath
      }

      DelegatingEngine.getTemplate(templateName).success
    } catch {
      case NonFatal(e) =>
        logger.error(s"Error while parsing Pebble template $resource", e)
        e.getMessage.failure
    }

  def evaluateTemplate(template: PebbleTemplate, session: Session): Validation[String] = {
    val context = sessionAttributesToJava(session.attributes)
    val writer = PooledSpecializedStringWriter.pooled
    try {
      template.evaluate(writer, context)
      writer.toString.success
    } catch {
      case NonFatal(e) =>
        logger.debug("Error while evaluating Pebble template", e)
        e.getMessage.failure
    }
  }
}
