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
package io.gatling.http.util

import java.nio.{ CharBuffer, ByteBuffer }
import java.nio.charset.{ CharacterCodingException, Charset }

import scala.annotation.switch

import io.gatling.commons.util.Collections._

import io.netty.buffer.ByteBuf

object BytesHelper {

  def byteBufsToBytes(bufs: Seq[ByteBuf]): Array[Byte] = {
    val bytes = new Array[Byte](bufs.sumBy(_.readableBytes))

    var pos = 0
    bufs.foreach { buf =>
      val i = buf.readableBytes
      buf.getBytes(0, bytes, pos, i)
      pos += i
    }

    bytes
  }

  def byteArraysToByteArray(arrays: Seq[Array[Byte]]): Array[Byte] =
    (arrays.length: @switch) match {
      case 0 => Array.empty
      case 1 => arrays.head
      case _ =>
        val all = new Array[Byte](arrays.sumBy(_.length))
        var pos = 0
        arrays.foreach { array =>
          System.arraycopy(array, 0, all, pos, array.length)
          pos += array.length
        }

        all
    }

  def byteBufsToString(bufs: Seq[ByteBuf], cs: Charset): String =
    byteBuffersToString(bufs.flatMap(_.nioBuffers), cs)

  def byteArraysToString(bufs: Seq[Array[Byte]], cs: Charset): String =
    byteBuffersToString(bufs.map(ByteBuffer.wrap), cs)

  def byteBuffersToString(bufs: Seq[ByteBuffer], cs: Charset): String = {

    val cd = cs.newDecoder
    val len = bufs.sumBy(_.remaining)
    val en = (len * cd.maxCharsPerByte.toDouble).toInt
    val ca = new Array[Char](en)
    cd.reset()
    val cb = CharBuffer.wrap(ca)

    val bbIt = bufs.iterator

    bbIt.foreach { buf =>

      try {
        var cr = cd.decode(buf, cb, !bbIt.hasNext)
        if (!cr.isUnderflow)
          cr.throwException()
        cr = cd.flush(cb)
        if (!cr.isUnderflow)
          cr.throwException()
      } catch {
        case x: CharacterCodingException => throw new Error(x)
      }
    }

    new String(ca, 0, cb.position)
  }

  val EmptyBytes = new Array[Byte](0)
}
