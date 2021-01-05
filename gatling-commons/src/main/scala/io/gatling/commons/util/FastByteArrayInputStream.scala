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

package io.gatling.commons.util

import java.io.InputStream

final class FastByteArrayInputStream(bytes: Array[Byte]) extends InputStream {

  private val length: Int = bytes.length
  private var position: Int = _
  private var mark: Int = _

  override def markSupported = true

  override def reset(): Unit = position = mark

  override def close(): Unit = {}

  override def mark(dummy: Int): Unit = mark = position

  override def available: Int = length - position

  override def skip(n: Long): Long = {
    val av = available
    if (n <= av) {
      position += n.toInt
      n
    } else {
      val n = av
      position = length
      n.toLong
    }
  }

  override def read(): Int =
    if (position == length) {
      -1
    } else {
      val oldPosition = position
      position += 1
      bytes(oldPosition) & 0xff
    }

  override def read(b: Array[Byte], offset: Int, length: Int): Int =
    if (position == this.length) {
      if (length == 0) 0 else -1
    } else {
      val n = math.min(length, available)
      System.arraycopy(bytes, offset + position, b, offset, n)
      position += n
      n
    }
}
