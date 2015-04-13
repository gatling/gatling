/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.metrics.message

import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.charset.StandardCharsets.UTF_8

import akka.util.{ ByteStringBuilder, ByteString }

sealed trait GraphiteMetric {
  def byteString: ByteString
}

private[metrics] case class PlainTextMetric(path: String, value: Long, epoch: Long) extends GraphiteMetric {
  def byteString = ByteString(s"$path $value $epoch\n", UTF_8.name)
}

private[metrics] object OpCodes {

  val Proto: Short = 0x80
  val Tuple2: Short = 0x86
  val Mark: Short = '('
  val Stop: Short = '.'
  val EmptyList: Short = ']'
  val Appends: Short = 'e'
  val Int: Short = 'I' // push integer or bool; decimal string argument
  val BinInt: Short = 'J' // push four-byte signed int (little endian)
  val BinInt1: Short = 'K' // push 1-byte unsigned int
  val BinInt2: Short = 'M' // push 2-byte unsigned int
  val BinUnicode: Short = 'X' //push Unicode string; counted UTF-8 string argument
  val Lf = '\n'
}

private[metrics] case class PickleMetric(metrics: Seq[PlainTextMetric]) extends GraphiteMetric {

  def byteString = {

    implicit val byteOrder = LITTLE_ENDIAN
    val bsb = new ByteStringBuilder()

      def int2Bytes(i: Int): Array[Byte] = {

        var vari = i

        val b = new Array[Byte](4)
        b(0) = (vari & 0xff).asInstanceOf[Byte]
        vari = vari >> 8
        b(1) = (vari & 0xff).asInstanceOf[Byte]
        vari = vari >> 8
        b(2) = (vari & 0xff).asInstanceOf[Byte]
        vari = vari >> 8
        b(3) = (vari & 0xff).asInstanceOf[Byte]
        b
      }

      def putString(string: String): Unit = {
        bsb.putInt(OpCodes.BinUnicode)
        val encoded = string.getBytes(UTF_8.name)
        bsb.putBytes(int2Bytes(encoded.length))
        bsb.putBytes(encoded)
      }

      def putLong(long: Long): Unit = {

        // choose optimal representation
        // first check 1 and 2-byte unsigned ints:
        if (long >= 0) {
          if (long <= 0xff) {
            bsb.putInt(OpCodes.BinInt1)
            bsb.putInt(long.toInt)
            return

          } else if (long <= 0xffff) {
            bsb.putInt(OpCodes.BinInt2)
            bsb.putInt((long & 0xff).toInt)
            bsb.putInt((long >> 8).toInt)
            return
          }
        }

        // 4-byte signed int?
        val highBits = long >> 31 // shift sign extends

        if (highBits == 0 || highBits == -1) {
          // All high bits are copies of bit 2**31, so the value fits in a 4-byte signed int.
          bsb.putInt(OpCodes.BinInt)
          bsb.putBytes(int2Bytes(long.toInt))

        } else {
          // int too big, store it as text
          bsb.putInt(OpCodes.Int)
          bsb.putBytes(String.valueOf(long).getBytes)
          bsb.putInt(OpCodes.Lf)
        }
      }

    bsb.putInt(OpCodes.Proto).putInt(2) // protocol version: 2

    bsb.putInt(OpCodes.EmptyList)
    bsb.putInt(OpCodes.Mark)

    // [(path, (timestamp, value)), ...]
    for (metric <- metrics) {
      putString(metric.path)
      putLong(metric.epoch)
      putLong(metric.value)
      bsb.putInt(OpCodes.Tuple2) // close inner tuple2
      bsb.putInt(OpCodes.Tuple2) // close outer tuple2
    }

    bsb.putInt(OpCodes.Appends)
    bsb.putInt(OpCodes.Stop)

    val payload = bsb.result
    val header = new ByteStringBuilder().putInt(payload.length).result
    header ++ payload
  }
}
