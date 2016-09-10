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

import io.gatling.commons.util.Collections._

import io.netty.buffer.ByteBuf

object ByteBufs {

  def byteBufsToByteArray(bufs: Seq[ByteBuf]): Array[Byte] = {
    // should be more efficient than creating a CompositeByteBuf
    val size = bufs.sumBy(_.readableBytes)
    val bytes = new Array[Byte](size)

    var offset = 0

    bufs.foreach { buf =>
      buf.getBytes(offset, bytes)
      offset += buf.readableBytes
    }

    bytes
  }
}
