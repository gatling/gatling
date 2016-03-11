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
package io.gatling.http.util

import java.nio.ByteBuffer
import java.nio.charset.{ CharacterCodingException, Charset }
import java.nio.charset.StandardCharsets._
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{ Function => JFunction }

import scala.compat.java8.FunctionConverters._

import io.gatling.commons.util.Collections._

object ByteBuffersDecoder {

  private[this] val utf8Decoders = new ThreadLocal[ByteBuffersDecoder] {
    override def initialValue = new Utf8ByteBuffersDecoder
  }

  private[this] val usAsciiDecoders = new ThreadLocal[ByteBuffersDecoder] {
    override def initialValue = new UsAsciiByteBuffersDecoder
  }

  private[this] val newCharsetDecodersScalaFunction: Charset => ThreadLocal[ByteBuffersDecoder] = charset => new ThreadLocal[ByteBuffersDecoder] {
    override def initialValue = new CharsetDecoderByteBuffersDecoder(charset)
  }
  private[this] val newCharsetDecoders: JFunction[Charset, ThreadLocal[ByteBuffersDecoder]] = newCharsetDecodersScalaFunction.asJava

  private[this] val otherDecoders = new ConcurrentHashMap[Charset, ThreadLocal[ByteBuffersDecoder]]()

  private[this] def decoder(charset: Charset): ByteBuffersDecoder = charset match {
    case UTF_8    => utf8Decoders.get()
    case US_ASCII => usAsciiDecoders.get()
    case _        => otherDecoders.computeIfAbsent(charset, newCharsetDecoders).get()
  }

  def decode(bufs: Seq[ByteBuffer], charset: Charset): String =
    decoder(charset).decode(bufs)
}

sealed trait ByteBuffersDecoder {

  def decode(bufs: Seq[ByteBuffer]): String
}

class CharsetDecoderByteBuffersDecoder(charset: Charset) extends ByteBuffersDecoder {

  private[this] def mergeByteBuffers(bufs: Seq[ByteBuffer]): Array[Byte] = {

    val len = bufs.sumBy(_.remaining)
    val bytes = new Array[Byte](len)
    val merged = ByteBuffer.wrap(bytes)
    bufs.foreach(merged.put)
    bytes
  }

  override def decode(bufs: Seq[ByteBuffer]): String =
    new String(mergeByteBuffers(bufs), charset)
}

class UsAsciiByteBuffersDecoder extends ByteBuffersDecoder {

  private val sb = new StringBuilder

  override def decode(bufs: Seq[ByteBuffer]): String = {
    sb.setLength(0)
    bufs.foreach { buf =>
      while (buf.remaining > 0) {
        sb.append(buf.get().toChar)
      }
    }
    sb.toString
  }
}

object Utf8ByteBuffersDecoder {
  val Utf8Accept = 0
  val Utf8Reject = 12

  val Types = Array[Byte](
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    8, 8, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    10, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 3, 3, 11, 6, 6, 6, 5, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8
  )

  val States = Array[Byte](
    0, 12, 24, 36, 60, 96, 84, 12, 12, 12, 48, 72, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    12, 0, 12, 12, 12, 12, 12, 0, 12, 0, 12, 12, 12, 24, 12, 12, 12, 12, 12, 24, 12, 24, 12, 12,
    12, 12, 12, 12, 12, 12, 12, 24, 12, 12, 12, 12, 12, 24, 12, 12, 12, 12, 12, 12, 12, 24, 12, 12,
    12, 12, 12, 12, 12, 12, 12, 36, 12, 36, 12, 12, 12, 36, 12, 12, 12, 12, 12, 36, 12, 36, 12, 12,
    12, 36, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12
  )
}

class Utf8ByteBuffersDecoder extends ByteBuffersDecoder {

  import Utf8ByteBuffersDecoder._

  private val sb = new StringBuilder
  private var state = Utf8Accept
  private var codep = 0

  private def write(b: Byte): Unit = {
    val t = Types(b & 0xFF)

    codep = if (state != Utf8Accept) (b & 0x3f) | (codep << 6) else (0xff >> t) & b
    state = States(state + t)

    if (state == Utf8Accept) {
      if (codep < Character.MIN_HIGH_SURROGATE) {
        sb.append(codep.toChar)
      } else {
        Character.toChars(codep).foreach(sb.append)
      }
    } else if (state == Utf8Reject) {
      throw new CharacterCodingException
    }
  }

  override def decode(bufs: Seq[ByteBuffer]): String = {

    sb.setLength(0)
    bufs.foreach { buf =>
      while (buf.remaining > 0) {
        write(buf.get())
      }
    }

    if (state == Utf8Accept) {
      sb.toString
    } else {
      throw new CharacterCodingException
    }
  }
}
