/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
import java.nio.charset.{ MalformedInputException, Charset }
import java.nio.charset.StandardCharsets._

import scala.annotation.switch

import io.gatling.commons.util.{ CompositeByteArrayInputStream, FastByteArrayInputStream }
import io.gatling.http.util.BytesHelper._

import com.typesafe.scalalogging.{ LazyLogging, StrictLogging }
import io.netty.buffer.ByteBuf

sealed trait ResponseBodyUsage
case object StringResponseBodyUsage extends ResponseBodyUsage
case object ByteArrayResponseBodyUsage extends ResponseBodyUsage
case object InputStreamResponseBodyUsage extends ResponseBodyUsage

trait ResponseBodyUsageStrategy {
  def bodyUsage(bodyLength: Int): ResponseBodyUsage
}

object StringResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  def bodyUsage(bodyLength: Int) = StringResponseBodyUsage
}

object ByteArrayResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  def bodyUsage(bodyLength: Int) = ByteArrayResponseBodyUsage
}

object InputStreamResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
  def bodyUsage(bodyLength: Int) = InputStreamResponseBodyUsage
}

sealed trait ResponseBody {
  def string: String
  def bytes: Array[Byte]
  def stream: InputStream
}

object StringResponseBody extends StrictLogging {

  def apply(chunks: Seq[ByteBuf], charset: Charset) = {
    val string =
      try {
        byteBufsToString(chunks, charset)
      } catch {
        case e: MalformedInputException =>
          logger.error(s"Response body is not valid ${charset.name} bytes")
          ""
      }
    new StringResponseBody(string, charset)
  }
}

class StringResponseBody(val string: String, charset: Charset) extends ResponseBody {

  lazy val bytes = string.getBytes(charset)
  def stream = new ByteArrayInputStream(bytes)
}

object ByteArrayResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset) = {
    val bytes = byteBufsToBytes(chunks)
    new ByteArrayResponseBody(bytes, charset)
  }
}

class ByteArrayResponseBody(val bytes: Array[Byte], charset: Charset) extends ResponseBody {

  def stream = new FastByteArrayInputStream(bytes)
  lazy val string = new String(bytes, charset)
}

object InputStreamResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset) = {

    val bytes = chunks.map { chunk =>
      val array = new Array[Byte](chunk.readableBytes)
      chunk.readBytes(array)
      array
    }

    new InputStreamResponseBody(bytes, charset)
  }
}

class InputStreamResponseBody(chunks: Seq[Array[Byte]], charset: Charset) extends ResponseBody with LazyLogging {

  def stream = (chunks.size: @switch) match {

    case 0 => new FastByteArrayInputStream(EmptyBytes)
    case 1 => new ByteArrayInputStream(chunks.head)
    case _ => new CompositeByteArrayInputStream(chunks)
  }

  private var bytesLoaded = false
  lazy val bytes = {
    bytesLoaded = true
    byteArraysToByteArray(chunks)
  }

  lazy val string = {
    if (bytesLoaded)
      new String(bytes, charset)
    else
      try {
        byteArraysToString(chunks, charset)
      } catch {
        case e: MalformedInputException =>
          logger.error(s"Response body is not valid ${charset.name} bytes")
          ""
      }
  }
}

object NoResponseBody extends ResponseBody {
  val charset = UTF_8
  val bytes = EmptyBytes
  def stream = new FastByteArrayInputStream(bytes)
  val string = ""
}
