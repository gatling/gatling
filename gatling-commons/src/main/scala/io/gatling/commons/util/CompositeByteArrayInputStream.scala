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

import io.gatling.commons.util.Collections._

class CompositeByteArrayInputStream(parts: Seq[Array[Byte]]) extends InputStream {
  require(parts.nonEmpty && parts.forall(_.nonEmpty), "Can't create a CompositeByteArrayInputStream with empty parts")

  private var currentPos = 0
  private var bytePos = -1
  private var active: Array[Byte] = parts.head
  private var _available = parts.sumBy(_.length)

  override val available: Int = _available

  override def read(): Int = {
    if (_available > 0) {
      bytePos += 1

      if (bytePos >= active.length) {
        // No more bytes, so step to the next array
        currentPos += 1
        if (currentPos >= parts.size) {
          _available = 0
        } else {
          bytePos = 0
          active = parts(currentPos)
        }
      }
    }

    if (_available == 0) {
      -1
    } else {
      _available -= 1
      active(bytePos) & 0xff
    }
  }
}
