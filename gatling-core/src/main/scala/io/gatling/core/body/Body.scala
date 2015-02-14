/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.body

import java.io.{ File => JFile, InputStream }

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.el.ElCompiler
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.Io._
import io.gatling.core.util.StringHelper._

object ElFileBody {
  def apply(filePath: Expression[String]) = CompositeByteArrayBody(ElFileBodies.asBytesSeq(filePath))
}

sealed trait Body

case class StringBody(string: Expression[String]) extends Body with Expression[String] {

  def apply(session: Session) = string(session)

  def asBytes: ByteArrayBody = ByteArrayBody(string.map(_.getBytes(configuration.core.charset)))
}

object RawFileBody {

  def apply(filePath: Expression[String]) = new RawFileBody(RawFileBodies.asFile(filePath))

  def unapply(b: RawFileBody) = Some(b.file)
}

class RawFileBody(val file: Expression[JFile]) extends Body with Expression[String] {

  def apply(session: Session) = asString(session)

  def asString: StringBody = StringBody(file.map(_.toString(configuration.core.charset)))

  def asBytes: ByteArrayBody = ByteArrayBody(file.map(_.toByteArray()))
}

case class ByteArrayBody(bytes: Expression[Array[Byte]]) extends Body

object CompositeByteArrayBody {
  def apply(string: String) = new CompositeByteArrayBody(ElCompiler.compile2BytesSeq(string))
}

case class CompositeByteArrayBody(bytes: Expression[Seq[Array[Byte]]]) extends Body with Expression[String] {

  def apply(session: Session) = bytes(session).map { bs =>
    val sb = stringBuilder
    bs.foreach(b => sb.append(new String(b, configuration.core.charset)))
    sb.toString
  }
}

case class InputStreamBody(is: Expression[InputStream]) extends Body
