/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import java.io.{ ByteArrayInputStream, InputStream }
import java.nio.charset.Charset

import scala.annotation.switch
import scala.util.control.NonFatal

import io.gatling.commons.util.{ CompositeByteArrayInputStream, FastByteArrayInputStream }
import io.gatling.commons.util.ByteBufs._
import io.gatling.commons.util.Bytes._

import com.typesafe.scalalogging.{ LazyLogging, StrictLogging }
import io.netty.buffer.ByteBuf
import org.asynchttpclient.netty.util.ByteBufUtils

sealed trait ResponseBodyUsage
case object StringResponseBodyUsage extends ResponseBodyUsage
case object ByteArrayResponseBodyUsage extends ResponseBodyUsage
case object InputStreamResponseBodyUsage extends ResponseBodyUsage

trait ResponseBodyUsageStrategy {
  def bodyUsage(bodyLength: Int): ResponseBodyUsage
}

object StringResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  override def bodyUsage(bodyLength: Int): ResponseBodyUsage = StringResponseBodyUsage
}

object ByteArrayResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  override def bodyUsage(bodyLength: Int): ResponseBodyUsage = ByteArrayResponseBodyUsage
}

object InputStreamResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  override def bodyUsage(bodyLength: Int): ResponseBodyUsage = InputStreamResponseBodyUsage
}

sealed trait ResponseBody {
  def string: String
  def bytes: Array[Byte]
  def stream: InputStream
}

object StringResponseBody extends StrictLogging {

  def apply(chunks: Seq[ByteBuf], charset: Charset): StringResponseBody = {
    val string =
      (chunks.length: @switch) match {
        case 0 => ""
        case 1 =>
          ByteBufUtils.byteBuf2String(charset, chunks.head)
        case _ =>
          ByteBufUtils.byteBuf2String(charset, chunks: _*)
      }
    new StringResponseBody(string, charset)
  }
}

class StringResponseBody(val string: String, charset: Charset) extends ResponseBody {

  override lazy val bytes: Array[Byte] = string.getBytes(charset)
  override def stream: ByteArrayInputStream = new ByteArrayInputStream(bytes)
}

object ByteArrayResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset): ByteArrayResponseBody =
    new ByteArrayResponseBody(byteBufsToByteArray(chunks), charset)
}

class ByteArrayResponseBody(val bytes: Array[Byte], charset: Charset) extends ResponseBody {

  override def stream: InputStream = new FastByteArrayInputStream(bytes)
  override lazy val string: String = byteArrayToString(bytes, charset)
}

object InputStreamResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset): InputStreamResponseBody = {
    val bytes = chunks.map(byteBufToByteArray)
    new InputStreamResponseBody(bytes, charset)
  }
}

class InputStreamResponseBody(chunks: Seq[Array[Byte]], charset: Charset) extends ResponseBody with LazyLogging {

  override def stream: InputStream =
    (chunks.size: @switch) match {
      case 0 => new FastByteArrayInputStream(Array.emptyByteArray)
      case 1 => new ByteArrayInputStream(chunks.head)
      case _ => new CompositeByteArrayInputStream(chunks)
    }

  override lazy val bytes: Array[Byte] = byteArraysToByteArray(chunks)

  override lazy val string: String =
    try {
      byteArraysToString(chunks, charset)
    } catch {
      case NonFatal(e) =>
        logger.error(s"Response body is not valid ${charset.name} bytes", e)
        ""
    }
}

case object NoResponseBody extends ResponseBody {
  override val bytes: Array[Byte] = Array.emptyByteArray
  override def stream: FastByteArrayInputStream = new FastByteArrayInputStream(bytes)
  override val string: String = ""
}
