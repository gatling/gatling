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

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets._

import scala.annotation.switch
import scala.collection.JavaConversions._

import io.gatling.commons.util.Collections._

import io.netty.buffer.{ ByteBuf, Unpooled }
import org.asynchttpclient.util.{ UsAsciiByteBufDecoder, Utf8ByteBufDecoder }

object BytesHelper {

  val EmptyBytes = Array.empty[Byte]

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
    cs match {
      case UTF_8    => Utf8ByteBufDecoder.getCachedDecoder.decode(bufs)
      case US_ASCII => UsAsciiByteBufDecoder.getCachedDecoder.decode(bufs)
      case _ =>
        var composite: ByteBuf = null
        try {
          composite = Unpooled.wrappedBuffer(bufs.map(_.retain()).toArray: _*)
          composite.toString(cs)
        } finally {
          if (composite != null) {
            composite.release()
          }
        }
    }

  def byteArraysToString(bytes: Seq[Array[Byte]], cs: Charset): String = {
    val bufs = bytes.map(Unpooled.wrappedBuffer)
    val string = byteBufsToString(bufs, cs)
    bufs.foreach(_.release())
    string
  }
}
