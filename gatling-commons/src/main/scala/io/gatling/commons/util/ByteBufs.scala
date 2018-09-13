/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.util

import io.gatling.commons.util.Collections._

import io.netty.buffer.ByteBuf

object ByteBufs {

  def byteBufToByteArray(buffer: ByteBuf): Array[Byte] =
    if (buffer.isReadable) {
      val byteArray = new Array[Byte](buffer.readableBytes)
      buffer.getBytes(buffer.readerIndex, byteArray)
      byteArray
    } else {
      Array.emptyByteArray
    }

  def byteBufsToByteArray(bufs: Seq[ByteBuf]): Array[Byte] =
    if (bufs.nonEmpty) {
      // should be more efficient than creating a CompositeByteBuf
      val size = bufs.sumBy(_.readableBytes)
      val bytes = new Array[Byte](size)

      var offset = 0

      bufs.foreach { buf =>
        val bufSize = buf.readableBytes
        buf.getBytes(0, bytes, offset, bufSize)
        offset += bufSize
      }

      bytes
    } else {
      Array.emptyByteArray
    }
}
