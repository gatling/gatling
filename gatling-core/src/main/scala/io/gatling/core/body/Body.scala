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

import java.io.InputStream
import java.nio.charset.Charset

import io.gatling.commons.util.CompositeByteArrayInputStream
import io.gatling.commons.validation._
import io.gatling.core.session._
import io.gatling.core.session.el.{ ElCompiler, ElParserException, StaticPart }
import io.gatling.netty.util.{ StringBuilderPool, StringWithCachedBytes }

sealed trait Body

final case class StringBody(string: Expression[String], charset: Charset) extends Body with Expression[String] {

  override def apply(session: Session): Validation[String] = string(session)
}

object RawFileBody {
  def apply(filePath: Expression[String], rawFileBodies: RawFileBodies): RawFileBody =
    new RawFileBody(rawFileBodies.asResourceAndCachedBytes(filePath))
}

final case class RawFileBody(resourceAndCachedBytes: Expression[ResourceAndCachedBytes]) extends Body with Expression[Array[Byte]] {
  override def apply(session: Session): Validation[Array[Byte]] =
    resourceAndCachedBytes(session).map(resourceAndCachedBytes => resourceAndCachedBytes.cachedBytes.getOrElse(resourceAndCachedBytes.resource.bytes))
}

final case class ByteArrayBody(bytes: Expression[Array[Byte]]) extends Body with Expression[Array[Byte]] {
  override def apply(session: Session): Validation[Array[Byte]] =
    bytes(session)
}

object ElBody {
  sealed trait ElBodyPart extends Product with Serializable
  final case class StaticElBodyPart(stringWithCachedBytes: StringWithCachedBytes) extends ElBodyPart
  final case class DynamicElBodyPart(string: Expression[String], charset: Charset) extends ElBodyPart

  @throws[ElParserException]
  private[body] def toParts(string: String, charset: Charset): List[ElBody.ElBodyPart] =
    ElCompiler.parse(string).map {
      case StaticPart(string) => StaticElBodyPart(new StringWithCachedBytes(string, charset))
      case part               => DynamicElBodyPart(part.map(_.toString), charset)
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
          case ElBody.StaticElBodyPart(stringWithCachedBytes) => sbV.map(_.append(stringWithCachedBytes.string))
          case ElBody.DynamicElBodyPart(stringE, _) =>
            for {
              sb <- sbV
              string <- stringE(session)
            } yield sb.append(string)
        }
      }
    } yield stringBuilder.toString

  def asStringWithCachedBytes: Expression[Seq[StringWithCachedBytes]] =
    session =>
      for {
        parts <- partsE(session)
        reversedBytes <- parts.foldLeft(List.empty[StringWithCachedBytes].success) { (accV, elPart) =>
          elPart match {
            case ElBody.StaticElBodyPart(stringWithCachedBytes) => accV.map(stringWithCachedBytes :: _)
            case ElBody.DynamicElBodyPart(stringE, charset) =>
              for {
                acc <- accV
                string <- stringE(session)
              } yield new StringWithCachedBytes(string, charset) :: acc
          }
        }
      } yield reversedBytes.reverse

  def asStream: Expression[InputStream] =
    asStringWithCachedBytes.map(stringWithCachedBytes => new CompositeByteArrayInputStream(stringWithCachedBytes.map(_.bytes)))
}

final case class InputStreamBody(is: Expression[InputStream]) extends Body

object PebbleStringBody {
  def apply(string: String, charset: Charset): StringBody = {
    val template = Pebble.getStringTemplate(string)
    StringBody(session => template.flatMap(Pebble.evaluateTemplate(_, session)), charset)
  }
}

object PebbleFileBody {
  def apply(filePath: Expression[String], pebbleFileBodies: PebbleFileBodies, charset: Charset): StringBody = {
    val template = pebbleFileBodies.asTemplate(filePath)
    StringBody(session => template(session).flatMap(Pebble.evaluateTemplate(_, session)), charset)
  }
}
