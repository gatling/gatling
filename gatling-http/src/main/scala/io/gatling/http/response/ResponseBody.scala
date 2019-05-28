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

package io.gatling.http.response

import java.io.{ InputStream, SequenceInputStream }
import java.nio.charset.Charset

import scala.annotation.switch
import scala.util.control.NonFatal
import scala.collection.JavaConverters._

import io.gatling.commons.util.FastByteArrayInputStream
import io.gatling.commons.util.ByteBufs._
import io.gatling.netty.util.ahc.ByteBufUtils._

import com.typesafe.scalalogging.LazyLogging
import io.netty.buffer.{ ByteBuf, ByteBufInputStream }

object ResponseBody {
  def apply(chunks: Seq[ByteBuf], charset: Charset): ResponseBody =
    (chunks.size: @switch) match {
      case 0 => NoResponseBody
      case 1 => new ByteBufResponseBody(chunks.head, charset)
      case _ => new ByteBufsResponseBody(chunks, charset)
    }
}

sealed trait ResponseBody {
  def string: String
  def chars: Array[Char]
  def bytes: Array[Byte]
  def stream: InputStream
}

class ByteBufResponseBody(chunk: ByteBuf, charset: Charset) extends ResponseBody with LazyLogging {

  override lazy val string: String =
    try {
      byteBuf2String(charset, chunk.duplicate)
    } catch {
      case NonFatal(e) =>
        logger.error(s"Response body is not valid ${charset.name} bytes", e)
        ""
    }

  override lazy val chars: Array[Char] =
    byteBuf2Chars(charset, chunk.duplicate)

  override lazy val bytes: Array[Byte] =
    byteBuf2Bytes(chunk.duplicate)

  override def stream: InputStream =
    new ByteBufInputStream(chunk.duplicate)
}

class ByteBufsResponseBody(chunks: Seq[ByteBuf], charset: Charset) extends ResponseBody with LazyLogging {

  override lazy val string: String =
    try {
      byteBuf2String(charset, chunks.map(_.duplicate): _*)
    } catch {
      case NonFatal(e) =>
        logger.error(s"Response body is not valid ${charset.name} bytes", e)
        ""
    }

  override lazy val chars: Array[Char] =
    byteBuf2Chars(charset, chunks.map(_.duplicate): _*)

  override lazy val bytes: Array[Byte] =
    byteBufsToByteArray(chunks.map(_.duplicate))

  override def stream: InputStream =
    new SequenceInputStream(chunks.map(chunk => new ByteBufInputStream(chunk.duplicate)).iterator.asJavaEnumeration)
}

case object NoResponseBody extends ResponseBody {
  override val string: String = ""
  override val chars: Array[Char] = Array.emptyCharArray
  override val bytes: Array[Byte] = Array.emptyByteArray
  override def stream: FastByteArrayInputStream = new FastByteArrayInputStream(bytes)
}

// for ResponseTransformer
class StringResponseBody(val string: String, charset: Charset) extends ResponseBody {

  override lazy val chars: Array[Char] = string.toCharArray

  override lazy val bytes: Array[Byte] = string.getBytes(charset)

  override def stream: InputStream = new FastByteArrayInputStream(bytes)
}

// for ResponseTransformer
class ByteArrayResponseBody(val bytes: Array[Byte], charset: Charset) extends ResponseBody {

  override lazy val string: String = new String(bytes, charset)

  override lazy val chars: Array[Char] = string.toCharArray

  override def stream: InputStream = new FastByteArrayInputStream(bytes)
}
