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
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets._

import scala.annotation.switch

import io.gatling.commons.util.{ CompositeByteArrayInputStream, FastByteArrayInputStream }

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

object ResponseBody {

  def byteBufsToBytes(bufs: Seq[ByteBuf]): Array[Byte] = {
    val bytes = new Array[Byte](bufs.map(_.readableBytes).sum)

    var pos = 0
    bufs.foreach { buf =>
      val i = buf.readableBytes
      buf.getBytes(0, bytes, pos, i)
      pos += i
    }

    bytes
  }

  val EmptyBytes = new Array[Byte](0)
}

sealed trait ResponseBody {
  def string: String
  def bytes: Array[Byte]
  def stream: InputStream
}

object StringResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset) = {
    val bytes = ResponseBody.byteBufsToBytes(chunks)
    val string = new String(bytes, charset)
    new StringResponseBody(string, charset)
  }
}

class StringResponseBody(val string: String, charset: Charset) extends ResponseBody {

  lazy val bytes = string.getBytes(charset)
  def stream = new ByteArrayInputStream(bytes)
}

object ByteArrayResponseBody {

  def apply(chunks: Seq[ByteBuf], charset: Charset) = {
    val bytes = ResponseBody.byteBufsToBytes(chunks)
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

class InputStreamResponseBody(chunks: Seq[Array[Byte]], charset: Charset) extends ResponseBody {

  def stream = (chunks.size: @switch) match {

    case 0 => new FastByteArrayInputStream(ResponseBody.EmptyBytes)
    case 1 => new ByteArrayInputStream(chunks.head)
    case _ => new CompositeByteArrayInputStream(chunks)
  }

  lazy val bytes = {
    val all = new Array[Byte](chunks.map(_.length).sum)
    var pos = 0
    chunks.foreach { chunk =>
      System.arraycopy(chunk, 0, all, pos, chunk.length)
      pos += chunk.length
    }

    all
  }

  lazy val string = new String(bytes, charset)
}

object NoResponseBody extends ResponseBody {
  val charset = UTF_8
  val bytes = ResponseBody.EmptyBytes
  def stream = new FastByteArrayInputStream(bytes)
  val string = ""
}
