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

import java.io.InputStream

import io.gatling.commons.util.CompositeByteArrayInputStream
import io.gatling.commons.util.StringHelper._
import io.gatling.commons.validation.Validation
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.session.el.ElCompiler

object ElFileBody {
  def apply(filePath: Expression[String])(implicit configuration: GatlingConfiguration, elFileBodies: ElFileBodies) = CompositeByteArrayBody(elFileBodies.asBytesSeq(filePath))
}

sealed trait Body

case class StringBody(string: Expression[String])(implicit configuration: GatlingConfiguration) extends Body with Expression[String] {

  def apply(session: Session) = string(session)

  def asBytes: ByteArrayBody = ByteArrayBody(string.map(_.getBytes(configuration.core.charset)))
}

object RawFileBody {
  def apply(filePath: Expression[String])(implicit configuration: GatlingConfiguration, rawFileBodies: RawFileBodies): RawFileBody =
    new RawFileBody(rawFileBodies.asFileWithCachedBytes(filePath))

  def unapply(b: RawFileBody) = Some(b.fileWithCachedBytes)
}

class RawFileBody(val fileWithCachedBytes: Expression[FileWithCachedBytes])(implicit configuration: GatlingConfiguration, rawFileBodies: RawFileBodies) extends Body with Expression[Array[Byte]] {
  def apply(session: Session): Validation[Array[Byte]] = fileWithCachedBytes(session).map(_.bytes)
}

object ByteArrayBody {
  def apply(string: String)(implicit configuration: GatlingConfiguration) = new ByteArrayBody(string.getBytes(configuration.core.charset).expressionSuccess)
}

case class ByteArrayBody(bytes: Expression[Array[Byte]])(implicit configuration: GatlingConfiguration) extends Body with Expression[Array[Byte]] {

  def apply(session: Session): Validation[Array[Byte]] = bytes(session)
}

object CompositeByteArrayBody {
  def apply(string: String)(implicit configuration: GatlingConfiguration) = new CompositeByteArrayBody(ElCompiler.compile2BytesSeq(string, configuration.core.charset))
}

case class CompositeByteArrayBody(bytes: Expression[Seq[Array[Byte]]])(implicit configuration: GatlingConfiguration) extends Body with Expression[String] {

  def apply(session: Session): Validation[String] = bytes(session).map { bs =>
    val sb = stringBuilder()
    bs.foreach(b => sb.append(new String(b, configuration.core.charset)))
    sb.toString
  }

  def asStream: Expression[InputStream] = bytes.map(new CompositeByteArrayInputStream(_))
}

case class InputStreamBody(is: Expression[InputStream]) extends Body
