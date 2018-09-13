/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

import java.io.InputStream
import java.nio.charset.Charset

import io.gatling.commons.util.CompositeByteArrayInputStream
import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.session.el.ElCompiler
import io.gatling.netty.util.ahc.StringBuilderPool

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

case class ByteArrayBody(bytes: Expression[Array[Byte]]) extends Body with Expression[Array[Byte]] {
  def apply(session: Session): Validation[Array[Byte]] =
    bytes(session)
}

object CompositeByteArrayBody {
  def apply(string: String)(implicit configuration: GatlingConfiguration): CompositeByteArrayBody = {
    val charset = configuration.core.charset
    new CompositeByteArrayBody(ElCompiler.compile2BytesSeq(string, charset), charset)
  }
}

case class CompositeByteArrayBody(bytes: Expression[Seq[Array[Byte]]], charset: Charset) extends Body with Expression[String] {

  override def apply(session: Session): Validation[String] = bytes(session).map { bs =>
    val sb = StringBuilderPool.DEFAULT.get()
    bs.foreach(b => sb.append(new String(b, charset)))
    sb.toString
  }

  def asStream: Expression[InputStream] = bytes.map(new CompositeByteArrayInputStream(_))
}

case class InputStreamBody(is: Expression[InputStream]) extends Body

object PebbleStringBody {
  def apply(string: String)(implicit configuration: GatlingConfiguration): PebbleBody = {
    val template = Pebble.parseStringTemplate(string)
    PebbleBody(_ => template)
  }
}

object PebbleFileBody {
  def apply(filePath: Expression[String])(implicit configuration: GatlingConfiguration, pebbleFileBodies: PebbleFileBodies): PebbleBody =
    PebbleBody(pebbleFileBodies.asTemplate(filePath))
}

case class PebbleBody(template: Expression[PebbleTemplate]) extends Body with Expression[String] with StrictLogging {
  def apply(session: Session): Validation[String] =
    template(session).flatMap(Pebble.evaluateTemplate(_, session))
}
