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
package io.gatling.http.request

import java.io.{ File => JFile, InputStream }

import io.gatling.core.session.el.ElCompiler

import scala.collection.JavaConverters._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.generators.InputStreamBodyGenerator
import com.ning.http.util.StringUtils.stringBuilder

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.Io._
import io.gatling.core.validation.Validation

object ElFileBody {
  def apply(filePath: Expression[String]) = CompositeByteArrayBody(ElFileBodies.asBytesSeq(filePath))
}

trait Body {
  def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder]
}

case class StringBody(string: Expression[String]) extends Body with Expression[String] {

  def apply(session: Session) = string(session)

  def asBytes: ByteArrayBody = ByteArrayBody(string.map(_.getBytes(configuration.core.charset)))

  def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = string(session).map(requestBuilder.setBody)
}

object RawFileBody {

  def apply(filePath: Expression[String]) = new RawFileBody(RawFileBodies.asFile(filePath))

  def unapply(b: RawFileBody) = Some(b.file)
}

class RawFileBody(val file: Expression[JFile]) extends Body with Expression[String] {

  def apply(session: Session) = asString(session)

  def asString: StringBody = StringBody(file.map(_.toString(configuration.core.charset)))

  def asBytes: ByteArrayBody = ByteArrayBody(file.map(_.toByteArray()))

  def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = file(session).map(requestBuilder.setBody)
}

case class ByteArrayBody(bytes: Expression[Array[Byte]]) extends Body {
  def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = bytes(session).map(requestBuilder.setBody)
}

object CompositeByteArrayBody {
  def apply(string: String) = new CompositeByteArrayBody(ElCompiler.compile2BytesSeq(string))
}

case class CompositeByteArrayBody(bytes: Expression[Seq[Array[Byte]]]) extends Body with Expression[String] {

  def apply(session: Session) = bytes(session).map { bs =>
    val sb = stringBuilder
    bs.foreach(b => sb.append(new String(b, configuration.core.charset)))
    sb.toString
  }

  def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = bytes(session).map(bs => requestBuilder.setBody(bs.asJava))
}

case class InputStreamBody(is: Expression[InputStream]) extends Body {
  def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = is(session).map(is => requestBuilder.setBody(new InputStreamBodyGenerator(is)))
}
