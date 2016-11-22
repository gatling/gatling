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
package io.gatling.core.body

import java.io.{ File, InputStream }
import java.nio.charset.Charset
import java.util.{ HashMap => JHashMap, Map => JMap }

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import io.gatling.commons.util.StringHelper._
import io.gatling.commons.util.{ CompositeByteArrayInputStream, Io }
import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.session.el.ElCompiler
import io.gatling.core.session.pebble.StringBuilderWriter

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.loader.StringLoader
import com.mitchellbosecke.pebble.template.PebbleTemplate
import com.typesafe.scalalogging.StrictLogging

object ElFileBody {
  def apply(filePath: Expression[String])(implicit configuration: GatlingConfiguration, elFileBodies: ElFileBodies) =
    CompositeByteArrayBody(elFileBodies.asBytesSeq(filePath), configuration.core.charset)
}

sealed trait Body

case class StringBody(string: Expression[String])(implicit configuration: GatlingConfiguration) extends Body with Expression[String] {

  def apply(session: Session) = string(session)

  def asBytes: ByteArrayBody = ByteArrayBody(string.map(_.getBytes(configuration.core.charset)))
}

object RawFileBody {
  def apply(filePath: Expression[String])(implicit rawFileBodies: RawFileBodies): RawFileBody =
    new RawFileBody(rawFileBodies.asResourceAndCachedBytes(filePath))

  def unapply(b: RawFileBody) = Some(b.resourceAndCachedBytes)
}

class RawFileBody(val resourceAndCachedBytes: Expression[ResourceAndCachedBytes]) extends Body with Expression[Array[Byte]] {
  def apply(session: Session): Validation[Array[Byte]] =
    resourceAndCachedBytes(session).map(resourceAndCachedBytes => resourceAndCachedBytes.cachedBytes.getOrElse(resourceAndCachedBytes.resource.bytes))
}

object ByteArrayBody {
  def apply(string: String)(implicit configuration: GatlingConfiguration) =
    new ByteArrayBody(string.getBytes(configuration.core.charset).expressionSuccess)
}

case class ByteArrayBody(bytes: Expression[Array[Byte]]) extends Body with Expression[Array[Byte]] {

  def apply(session: Session): Validation[Array[Byte]] =
    bytes(session)
}

object CompositeByteArrayBody {
  def apply(string: String)(implicit configuration: GatlingConfiguration) = {
    val charset = configuration.core.charset
    new CompositeByteArrayBody(ElCompiler.compile2BytesSeq(string, charset), charset)
  }
}

case class CompositeByteArrayBody(bytes: Expression[Seq[Array[Byte]]], charset: Charset) extends Body with Expression[String] {

  def apply(session: Session): Validation[String] = bytes(session).map { bs =>
    val sb = stringBuilder()
    bs.foreach(b => sb.append(new String(b, charset)))
    sb.toString
  }

  def asStream: Expression[InputStream] = bytes.map(new CompositeByteArrayInputStream(_))
}

case class InputStreamBody(is: Expression[InputStream]) extends Body

object PebbleBody {
  implicit val Engine = new PebbleEngine.Builder().loader(new StringLoader).build

  def matchMap(map: Map[String, Any]): JMap[String, AnyRef] = {
    val jMap: JMap[String, AnyRef] = new JHashMap(map.size)
    for ((k, v) <- map) {
      v match {
        case c: Iterable[Any] => jMap.put(k, c.asJava)
        case any: AnyRef      => jMap.put(k, any) //The AnyVal case is not addressed, as an AnyVal will be in an AnyRef wrapper
      }
    }
    jMap
  }
}

object PebbleStringBody {
  def apply(string: String)(implicit configuration: GatlingConfiguration): PebbleStringBody = {
    val template = PebbleBody.Engine.getTemplate(string)
    implicit val charset = configuration.core.charset
    new PebbleStringBody(template)
  }
}

case class PebbleStringBody(template: PebbleTemplate)(implicit charset: Charset) extends Body with Expression[String] with StrictLogging {

  def apply(session: Session): Validation[String] = {
    val writer: StringBuilderWriter = StringBuilderWriter.getWriter(charset)
    val javaMap = PebbleBody.matchMap(session.attributes)

    try {
      template.evaluate(writer, javaMap)
      writer.toString.success
    } catch {
      case NonFatal(e) =>
        logger.error("Error while parsing Pebble String", e)
        e.getMessage.failure
    }
  }
}

object PebbleFileBody {
  def apply(filePath: String)(implicit configuration: GatlingConfiguration): PebbleFileBody = {
    val template = PebbleBody.Engine.getTemplate(new String(Io.RichFile(new File(filePath)).toByteArray))
    implicit val charset = configuration.core.charset
    new PebbleFileBody(template)
  }
}

case class PebbleFileBody(template: PebbleTemplate)(implicit charset: Charset) extends Body with Expression[String] with StrictLogging {

  def apply(session: Session): Validation[String] = {
    val writer: StringBuilderWriter = StringBuilderWriter.getWriter(charset)
    val javaMap = PebbleBody.matchMap(session.attributes)

    try {
      template.evaluate(writer, javaMap)
      writer.toString.success
    } catch {
      case NonFatal(e) =>
        logger.error("Error while parsing Pebble File", e)
        e.getMessage.failure
    }
  }
}
