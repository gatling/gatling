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

import java.io.InputStream

import io.netty.util.concurrent.FastThreadLocal

object UsAsciiInputStreamDecoder {

  private[this] val Pool = new FastThreadLocal[UsAsciiInputStreamDecoder] {
    override protected def initialValue(): UsAsciiInputStreamDecoder = new UsAsciiInputStreamDecoder
  }

  def pooled(): UsAsciiInputStreamDecoder = {
    val decoder = Pool.get()
    decoder.reset()
    decoder
  }
}

class UsAsciiInputStreamDecoder {

  private val sb = new java.lang.StringBuilder

  def decode(is: InputStream): String = {

    var b = is.read()
    while (b != -1) {
      sb.append(b)
      b = is.read()
    }

    sb.toString
  }

  def reset(): Unit = sb.setLength(0)
}
