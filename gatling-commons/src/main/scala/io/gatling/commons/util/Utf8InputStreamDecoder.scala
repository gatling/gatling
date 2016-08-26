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
import java.nio.charset.CharacterCodingException

import io.netty.util.concurrent.FastThreadLocal
import org.asynchttpclient.util.Utf8Decoder

object Utf8InputStreamDecoder {

  private[this] val Pool = new FastThreadLocal[Utf8InputStreamDecoder] {
    override protected def initialValue(): Utf8InputStreamDecoder = new Utf8InputStreamDecoder
  }

  def pooled(): Utf8InputStreamDecoder = {
    val decoder = Pool.get()
    decoder.reset()
    decoder
  }
}

class Utf8InputStreamDecoder extends Utf8Decoder {

  @throws[CharacterCodingException]
  def decode(is: InputStream): String = {

    var b = is.read()
    while (b != -1) {
      write(b.toByte)
      b = is.read()
    }

    if (state == Utf8Decoder.UTF8_ACCEPT) {
      sb.toString
    } else {
      throw new CharacterCodingException
    }
  }
}
