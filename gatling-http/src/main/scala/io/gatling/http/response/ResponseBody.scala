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

package io.gatling.http.response

import java.io.{ InputStream, SequenceInputStream }
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

import io.gatling.commons.util.ByteBufs._
import io.gatling.commons.util.FastByteArrayInputStream
import io.gatling.netty.util.ByteBufUtils._

import com.typesafe.scalalogging.LazyLogging
import io.netty.buffer.{ ByteBuf, ByteBufInputStream }

object ResponseBody {

  def apply(bodyLength: Int, chunks: List[ByteBuf], charset: Charset): ResponseBody =
    chunks match {
      case Nil          => NoResponseBody(bodyLength)
      case chunk :: Nil => new ByteBufResponseBody(bodyLength, chunk, charset)
      case _            => new ByteBufsResponseBody(bodyLength, chunks, charset)
    }
}

sealed trait ResponseBody {
  def length: Int
  def charset: Charset
  def string: String
  def chars: Array[Char]
  def bytes: Array[Byte]
  def stream: InputStream
}

private[gatling] final class ByteBufResponseBody(override val length: Int, chunk: ByteBuf, override val charset: Charset)
    extends ResponseBody
    with LazyLogging {

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

private[gatling] final class ByteBufsResponseBody(override val length: Int, chunks: Seq[ByteBuf], override val charset: Charset)
    extends ResponseBody
    with LazyLogging {

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

object NoResponseBody {
  val Empty: NoResponseBody = new NoResponseBody(0)

  def apply(length: Int): NoResponseBody =
    if (length == 0) Empty else new NoResponseBody(length)
}

final class NoResponseBody(val length: Int) extends ResponseBody {
  override val charset: Charset = UTF_8
  override val string: String = ""
  override val chars: Array[Char] = Array.emptyCharArray
  override val bytes: Array[Byte] = Array.emptyByteArray
  override def stream: FastByteArrayInputStream = new FastByteArrayInputStream(bytes)
}

// for ResponseTransformer
final class StringResponseBody(val string: String, override val charset: Charset) extends ResponseBody {

  override def length: Int = bytes.length

  override lazy val chars: Array[Char] = string.toCharArray

  override lazy val bytes: Array[Byte] = string.getBytes(charset)

  override def stream: InputStream = new FastByteArrayInputStream(bytes)
}

// for ResponseTransformer and PollerActor
final class ByteArrayResponseBody(val bytes: Array[Byte], override val charset: Charset) extends ResponseBody {

  override def length: Int = bytes.length

  override lazy val string: String = new String(bytes, charset)

  override lazy val chars: Array[Char] = string.toCharArray

  override def stream: InputStream = new FastByteArrayInputStream(bytes)
}
