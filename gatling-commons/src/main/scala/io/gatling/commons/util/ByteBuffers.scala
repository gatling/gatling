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
package io.gatling.commons.util

import java.nio.ByteBuffer

import io.gatling.commons.util.Collections._

object ByteBuffers {

  val Empty = ByteBuffer.wrap(Array.empty)

  def byteBuffer2ByteArray(byteBuffer: ByteBuffer): Array[Byte] = {
    val bytes = new Array[Byte](byteBuffer.remaining)
    if (byteBuffer.hasArray) {
      System.arraycopy(byteBuffer.array, byteBuffer.arrayOffset, bytes, 0, bytes.length)
    } else {
      byteBuffer.get(bytes)
    }

    bytes
  }

  def sumByteBuffers(buffers: Iterable[ByteBuffer]): ByteBuffer = {
    val comb = ByteBuffer.allocate(buffers.sumBy(_.remaining))
    copyInto(buffers, comb)
  }

  def copyInto(sources: Iterable[ByteBuffer], target: ByteBuffer): ByteBuffer = {
    sources.foreach(target.put)
    target.flip()
    target
  }
}
