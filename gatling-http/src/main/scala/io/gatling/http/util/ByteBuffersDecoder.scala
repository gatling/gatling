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

import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.charset.{ CoderResult, CharsetDecoder, Charset }
import java.nio.charset.StandardCharsets._
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{ Function => JFunction }

import io.gatling.commons.util.Collections._
import io.gatling.commons.util.JFunctions._

object ByteBuffersDecoder {

  private[this] val utf8Decoders = new ThreadLocal[ByteBuffersDecoder] {
    override def initialValue = new Utf8ByteBuffersDecoder
  }

  private[this] val usAsciiDecoders = new ThreadLocal[ByteBuffersDecoder] {
    override def initialValue = new UsAsciiByteBuffersDecoder
  }

  private[this] val newGenericDecoders: JFunction[Charset, ThreadLocal[ByteBuffersDecoder]] = (charset: Charset) => new ThreadLocal[ByteBuffersDecoder] {
    override def initialValue = new GenericByteBuffersDecoder(charset)
  }

  private[this] val otherDecoders = new ConcurrentHashMap[Charset, ThreadLocal[ByteBuffersDecoder]]()

  private[this] def decoder(charset: Charset): ByteBuffersDecoder = charset match {
    case UTF_8    => utf8Decoders.get()
    case US_ASCII => usAsciiDecoders.get()
    case _        => otherDecoders.computeIfAbsent(charset, newGenericDecoders).get()
  }

  def decode(bufs: Seq[ByteBuffer], charset: Charset): String =
    decoder(charset).decode(bufs)
}

sealed abstract class ByteBuffersDecoder(charset: Charset) {

  private[this] val decoder = charset.newDecoder
  private[this] var cachedChars: Array[Char] = _

  def decode(bufs: Seq[ByteBuffer]): String = {
    decoder.reset()

    val nonEmptyBufs = bufs.filter(_.remaining > 0)

    if (nonEmptyBufs.isEmpty) {
      ""

    } else {
      val len = nonEmptyBufs.sumBy(_.remaining)
      val chars =
        if (cachedChars == null) {
          // init cache
          cachedChars = new Array(len)
          cachedChars
        } else if (len < 200000) {
          // increase cache
          cachedChars = new Array(len)
          cachedChars
        } else {
          // too large, don't cache
          new Array[Char](len)
        }

      val charBuffer = CharBuffer.wrap(chars)
      decode0(nonEmptyBufs, charBuffer, decoder)

      val coderResult = decoder.flush(charBuffer)
      if (!coderResult.isUnderflow) {
        coderResult.throwException()
      }

      new String(chars, 0, charBuffer.position)
    }
  }

  protected def decode0(bufs: Seq[ByteBuffer], charBuffer: CharBuffer, charsetDecoder: CharsetDecoder): Unit
}

class GenericByteBuffersDecoder(charset: Charset) extends ByteBuffersDecoder(charset) {

  private[this] def mergeByteBuffers(bufs: Seq[ByteBuffer]): Array[Byte] = {

    val len = bufs.sumBy(_.remaining)
    val bytes = new Array[Byte](len)
    val merged = ByteBuffer.wrap(bytes)
    bufs.foreach(merged.put)
    bytes
  }

  override def decode(bufs: Seq[ByteBuffer]): String =
    new String(mergeByteBuffers(bufs), charset)

  override protected def decode0(bufs: Seq[ByteBuffer], charBuffer: CharBuffer, charsetDecoder: CharsetDecoder): Unit =
    throw new UnsupportedOperationException
}

class UsAsciiByteBuffersDecoder extends ByteBuffersDecoder(US_ASCII) {

  override protected def decode0(bufs: Seq[ByteBuffer], charBuffer: CharBuffer, charsetDecoder: CharsetDecoder): Unit = {

    val bufIt = bufs.iterator

    var coderResult: CoderResult = null
    bufIt.foreach { buf =>

      // decode rest of buffer
      coderResult = charsetDecoder.decode(buf, charBuffer, bufIt.hasNext)
      if (!coderResult.isUnderflow) {
        coderResult.throwException()
      }
    }
  }
}

class Utf8ByteBuffersDecoder extends ByteBuffersDecoder(UTF_8) {

  private[this] def peekByte(buf: ByteBuffer): Byte = {
    val b = buf.get()
    buf.position(buf.position - 1)
    b
  }

  private[this] def numberOfUtf8Bytes(firstByte: Byte, coderResult: CoderResult): Int =
    if (firstByte >= 0) {
      // 1 byte, impossible to be malformed!!!
      coderResult.throwException()
      1
    } else if (firstByte >= -16) {
      // 2 bytes
      2
    } else if (firstByte >= -32) {
      // 3 bytes
      3
    } else if (firstByte >= -64) {
      // 4 bytes
      4
    } else {
      // not a char first byte, malformed
      coderResult.throwException()
      1
    }

  private[this] def decodeSplitChar(buf1: ByteBuffer, buf2: ByteBuffer, charBytes: Int, charBuffer: CharBuffer, charsetDecoder: CharsetDecoder): Unit = {

    val temp = ByteBuffer.allocate(charBytes)
    val pendingBytes = buf1.remaining()
    // copy pending bytes
    temp.put(buf1)
    // copy missing bytes
    for (_ <- 0 until charBytes - pendingBytes)
      temp.put(buf2.get())

    // decode temp
    temp.flip()
    val coderResult = charsetDecoder.decode(temp, charBuffer, true)
    if (!coderResult.isUnderflow) {
      coderResult.throwException()
    }
  }

  override protected def decode0(bufs: Seq[ByteBuffer], charBuffer: CharBuffer, charsetDecoder: CharsetDecoder): Unit = {

    val bufIt = bufs.iterator

    var expectedBytes: Int = 0
    var pendingBuffer: ByteBuffer = null

    var coderResult: CoderResult = null
    bufIt.foreach { buf =>
      if (pendingBuffer != null) {
        decodeSplitChar(pendingBuffer, buf, expectedBytes, charBuffer, charsetDecoder)
        expectedBytes = 0
        pendingBuffer = null
      }

      // decode rest of buffer
      coderResult = charsetDecoder.decode(buf, charBuffer, true)
      if (!coderResult.isUnderflow) {
        if (bufIt.hasNext) {
          // compute pending bytes
          pendingBuffer = buf
          expectedBytes = numberOfUtf8Bytes(peekByte(buf), coderResult)
        } else {
          coderResult.throwException()
        }
      }
    }
  }
}
