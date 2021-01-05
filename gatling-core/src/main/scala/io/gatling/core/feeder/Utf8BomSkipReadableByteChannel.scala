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

package io.gatling.core.feeder

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

private object Utf8BomSkipReadableByteChannel {
  val Utf8BomByte1: Byte = 0xef.toByte
  val Utf8BomByte2: Byte = 0xbb.toByte
  val Utf8BomByte3: Byte = 0xbf.toByte
}

private class Utf8BomSkipReadableByteChannel(channel: ReadableByteChannel) extends ReadableByteChannel {

  import Utf8BomSkipReadableByteChannel._

  private val first3Bytes = {
    val bb = ByteBuffer.allocate(3)
    channel.read(bb)
    bb.flip()
    bb
  }
  private val hasBom =
    first3Bytes.remaining >= 3 &&
      first3Bytes.get(0) == Utf8BomByte1 &&
      first3Bytes.get(1) == Utf8BomByte2 &&
      first3Bytes.get(2) == Utf8BomByte3

  override def read(dst: ByteBuffer): Int =
    if (hasBom || !first3Bytes.hasRemaining) {
      channel.read(dst)
    } else {
      dst.put(first3Bytes)
      first3Bytes.position + channel.read(dst)
    }

  override def close(): Unit = channel.close()

  override def isOpen(): Boolean = channel.isOpen()
}
