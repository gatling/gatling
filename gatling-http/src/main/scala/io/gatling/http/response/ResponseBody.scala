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

import java.io.{ ByteArrayInputStream, InputStream }
import java.nio.charset.Charset

import scala.annotation.switch
import scala.util.control.NonFatal

import io.gatling.commons.util.{ CompositeByteArrayInputStream, FastByteArrayInputStream }
import io.gatling.commons.util.ByteBufs._
import io.gatling.commons.util.Bytes._
import io.gatling.commons.util.StringHelper._
import io.gatling.netty.util.ahc.ByteBufUtils._

import com.typesafe.scalalogging.LazyLogging
import io.netty.buffer.ByteBuf

sealed trait ResponseBodyUsage
case object StringResponseBodyUsage extends ResponseBodyUsage
case object CharArrayResponseBodyUsage extends ResponseBodyUsage
case object ByteArrayResponseBodyUsage extends ResponseBodyUsage
case object InputStreamResponseBodyUsage extends ResponseBodyUsage

trait ResponseBodyUsageStrategy {
  def bodyUsage(contentLength: Int): ResponseBodyUsage
}

object StringResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  override def bodyUsage(contentLength: Int): ResponseBodyUsage = StringResponseBodyUsage
}

object CharArrayResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  override def bodyUsage(contentLength: Int): ResponseBodyUsage = CharArrayResponseBodyUsage
}

object ByteArrayResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  override def bodyUsage(contentLength: Int): ResponseBodyUsage = ByteArrayResponseBodyUsage
}

object InputStreamResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  override def bodyUsage(contentLength: Int): ResponseBodyUsage = InputStreamResponseBodyUsage
}

sealed trait ResponseBody {
  def string: String
  def chars: Array[Char]
  def bytes: Array[Byte]
  def stream: InputStream
}

object StringResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset): StringResponseBody = {
    val string =
      (chunks.size: @switch) match {
        case 0 => ""
        case 1 => byteBuf2String(charset, chunks.head)
        case _ => byteBuf2String(charset, chunks: _*)
      }
    new StringResponseBody(string, charset)
  }
}

class StringResponseBody(val string: String, charset: Charset) extends ResponseBody {

  override lazy val chars: Array[Char] = string.unsafeChars
  override lazy val bytes: Array[Byte] = string.getBytes(charset)
  override def stream: ByteArrayInputStream = new ByteArrayInputStream(bytes)
}

object CharArrayResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset): CharArrayResponseBody = {
    val chars =
      (chunks.size: @switch) match {
        case 0 => Array.emptyCharArray
        case 1 => byteBuf2Chars(charset, chunks.head)
        case _ => byteBuf2Chars(charset, chunks: _*)
      }
    new CharArrayResponseBody(chars, charset)
  }
}

class CharArrayResponseBody(val chars: Array[Char], charset: Charset) extends ResponseBody {

  override lazy val string: String = new String(chars)
  override lazy val bytes: Array[Byte] = charArrayToByteArray(chars, charset)
  override def stream: InputStream = new FastByteArrayInputStream(bytes)
}

object ByteArrayResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset): ByteArrayResponseBody =
    new ByteArrayResponseBody(byteBufsToByteArray(chunks), charset)
}

class ByteArrayResponseBody(val bytes: Array[Byte], charset: Charset) extends ResponseBody {

  override lazy val string: String = byteArrayToString(bytes, charset)
  override lazy val chars: Array[Char] = string.toCharArray
  override def stream: InputStream = new FastByteArrayInputStream(bytes)
}

object InputStreamResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset): InputStreamResponseBody = {
    val bytes = chunks.map(byteBufToByteArray)
    new InputStreamResponseBody(bytes, charset)
  }
}

class InputStreamResponseBody(chunks: Seq[Array[Byte]], charset: Charset) extends ResponseBody with LazyLogging {

  override lazy val string: String =
    try {
      byteArraysToString(chunks, charset)
    } catch {
      case NonFatal(e) =>
        logger.error(s"Response body is not valid ${charset.name} bytes", e)
        ""
    }

  override lazy val chars: Array[Char] = string.toCharArray

  override lazy val bytes: Array[Byte] = byteArraysToByteArray(chunks)

  override def stream: InputStream =
    (chunks.size: @switch) match {
      case 0 => new FastByteArrayInputStream(Array.emptyByteArray)
      case 1 => new ByteArrayInputStream(chunks.head)
      case _ => new CompositeByteArrayInputStream(chunks)
    }
}

case object NoResponseBody extends ResponseBody {
  override val string: String = ""
  override val chars: Array[Char] = Array.emptyCharArray
  override val bytes: Array[Byte] = Array.emptyByteArray
  override def stream: FastByteArrayInputStream = new FastByteArrayInputStream(bytes)
}
