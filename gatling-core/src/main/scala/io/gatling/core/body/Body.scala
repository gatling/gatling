/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.validation._
import io.gatling.commons.util.CompositeByteArrayInputStream
import io.gatling.core.session._
import io.gatling.core.session.el.{ ElCompiler, ElParserException, StaticPart }
import io.gatling.netty.util.StringBuilderPool

import com.mitchellbosecke.pebble.template.PebbleTemplate

sealed trait Body

final case class StringBody(string: Expression[String], charset: Charset) extends Body with Expression[String] {

  override def apply(session: Session): Validation[String] = string(session)

  def asBytes: ByteArrayBody = ByteArrayBody(string.map(_.getBytes(charset)))
}

object RawFileBody {
  def apply(filePath: Expression[String], rawFileBodies: RawFileBodies): RawFileBody =
    new RawFileBody(rawFileBodies.asResourceAndCachedBytes(filePath))

  def unapply(b: RawFileBody): Option[Expression[ResourceAndCachedBytes]] = Some(b.resourceAndCachedBytes)
}

final class RawFileBody(val resourceAndCachedBytes: Expression[ResourceAndCachedBytes]) extends Body with Expression[Array[Byte]] {
  override def apply(session: Session): Validation[Array[Byte]] =
    resourceAndCachedBytes(session).map(resourceAndCachedBytes => resourceAndCachedBytes.cachedBytes.getOrElse(resourceAndCachedBytes.resource.bytes))
}

final case class ByteArrayBody(bytes: Expression[Array[Byte]]) extends Body with Expression[Array[Byte]] {
  override def apply(session: Session): Validation[Array[Byte]] =
    bytes(session)
}

object ElBody {
  sealed trait ElBodyPart extends Product with Serializable
  @SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
  final case class StaticElBodyPart(string: String, bytes: Array[Byte]) extends ElBodyPart
  final case class DynamicBytes(string: Expression[String], bytes: Expression[Array[Byte]]) extends ElBodyPart

  @throws[ElParserException]
  private[body] def toParts(string: String, charset: Charset): List[ElBody.ElBodyPart] =
    ElCompiler.parse(string).map {
      case StaticPart(string) => ElBody.StaticElBodyPart(string, string.getBytes(charset))
      case part               => DynamicBytes(part.map(_.toString), part.map(_.toString.getBytes(charset)))
    }

  def apply(string: String, charset: Charset): ElBody =
    ElBody(toParts(string, charset).expressionSuccess)
}

final case class ElBody(partsE: Expression[List[ElBody.ElBodyPart]]) extends Body with Expression[String] {

  override def apply(session: Session): Validation[String] =
    for {
      parts <- partsE(session)
      stringBuilder <- parts.foldLeft(StringBuilderPool.DEFAULT.get().success) { (sbV, elPart) =>
        elPart match {
          case ElBody.StaticElBodyPart(string, _) => sbV.map(_.append(string))
          case ElBody.DynamicBytes(stringE, _) =>
            for {
              sb <- sbV
              string <- stringE(session)
            } yield sb.append(string)
        }
      }
    } yield stringBuilder.toString

  def asBytes: Expression[Seq[Array[Byte]]] =
    session =>
      for {
        parts <- partsE(session)
        reversedBytes <- parts.foldLeft(List.empty[Array[Byte]].success) { (accV, elPart) =>
          elPart match {
            case ElBody.StaticElBodyPart(_, bytes) => accV.map(bytes :: _)
            case ElBody.DynamicBytes(_, bytesE) =>
              for {
                acc <- accV
                bytes <- bytesE(session)
              } yield bytes :: acc
          }
        }
      } yield reversedBytes.reverse

  def asStream: Expression[InputStream] =
    asBytes.map(new CompositeByteArrayInputStream(_))
}

final case class InputStreamBody(is: Expression[InputStream]) extends Body

object PebbleStringBody {
  def apply(string: String): PebbleBody = {
    val template = Pebble.getStringTemplate(string)
    PebbleBody(_ => template)
  }
}

object PebbleFileBody {
  def apply(filePath: Expression[String], pebbleFileBodies: PebbleFileBodies): PebbleBody =
    PebbleBody(pebbleFileBodies.asTemplate(filePath))
}

final case class PebbleBody(template: Expression[PebbleTemplate]) extends Body with Expression[String] {
  override def apply(session: Session): Validation[String] =
    template(session).flatMap(Pebble.evaluateTemplate(_, session))
}
