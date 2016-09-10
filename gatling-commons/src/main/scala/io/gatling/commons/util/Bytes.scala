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

import java.nio.charset.Charset
import scala.annotation.switch

import io.gatling.commons.util.Collections._

import io.netty.buffer.Unpooled
import org.asynchttpclient.netty.util.ByteBufUtils

object Bytes {

  val EmptyBytes = Array.empty[Byte]

  def byteArraysToByteArray(arrays: Seq[Array[Byte]]): Array[Byte] =
    (arrays.length: @switch) match {
      case 0 => EmptyBytes
      case 1 => arrays.head
      case _ =>
        val target = new Array[Byte](arrays.sumBy(_.length))
        var pos = 0
        arrays.foreach { array =>
          System.arraycopy(array, 0, target, pos, array.length)
          pos += array.length
        }

        target
    }

  def byteArrayToString(bytes: Array[Byte], cs: Charset): String = {
    val buf = Unpooled.wrappedBuffer(bytes)
    try {
      ByteBufUtils.byteBuf2String(cs, buf)
    } finally {
      buf.release()
    }
  }

  def byteArraysToString(bytes: Seq[Array[Byte]], cs: Charset): String =
    (bytes.length: @switch) match {
      case 0 => ""
      case 1 =>
        // FIXME to be done in AHC in 2.0.16
        byteArrayToString(bytes.head, cs)
      case _ =>
        val bufs = bytes.map(Unpooled.wrappedBuffer)
        try {
          ByteBufUtils.byteBuf2String(cs, bufs: _*)
        } finally {
          bufs.foreach(_.release())
        }
    }
}
